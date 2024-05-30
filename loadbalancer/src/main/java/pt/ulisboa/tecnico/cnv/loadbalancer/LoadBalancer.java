package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.Map;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;


public class LoadBalancer {

    private AmazonDynamoDB dynamoDB;
    private AmazonEC2 ec2;
    private AWSLambda lambda;
    private AutoScaler autoScaler;
    private List<String> activeWorkerIPs;
    List<Map<String, AttributeValue>> dynamoDBMetrics;


    public LoadBalancer(AmazonDynamoDB dynamoDB, AmazonEC2 ec2, AWSLambda lambda, AutoScaler autoScaler) {
        this.dynamoDB = dynamoDB;
        this.ec2 = ec2;
        this.lambda = lambda;
        this.autoScaler = autoScaler;
        this.activeWorkerIPs = new ArrayList<>();
    }

    public String handleRequest(String requestType) {
        double complexity = estimateComplexity(requestType);

        // TODO: Change the way the loadbalancer distributes the requests
        if (complexity < getThreshold()) {
            return invokeLambda(requestType);
        } else {
            String workerIP = estimateBestWorker();
            return forwardToWorker(requestType, workerIP);
        }
    }

    private double estimateComplexity(String requestType) {
        if (this.dynamoDB == null | this.dynamoDBMetrics == null){
            return 0;
        }

        double w1 = 0.5; // Executed Instructions
        double w2 = 0.3; // Executed Basic Blocks
        double w3 = 0.2; // Executed Methods
        
        int reqWeight = 0; // can be 1 if blurimage or enhanceimage, 2 if raytracer
        double complexity;

        if (requestType == "imageproc"){
            reqWeight = 1;
        }
        if (requestType == "raytracer"){
            reqWeight = 2;
        }

        double avgNumExecutedInstructions = getAvgNumExecutedInstructions(requestType);
        double avgNumExecutedBB = getAvgNumExecutedBB(requestType);
        double avgNumExecutedMethods = getAvgNumExecutedMethods(requestType);

        complexity = reqWeight * (w1*avgNumExecutedInstructions + w2*avgNumExecutedBB + w3*avgNumExecutedMethods);
        System.out.println("Request complexity of <" + requestType + "> is:" + complexity);

        return complexity;
    }

    private String estimateBestWorker(){
        // <IP, num de requests>
        Map<String, Integer> instancesComplexity = new HashMap<>();
        String bestWorkerIp = null;

        // populates map of ips and numof requests
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics){
            String ip = itemMetrics.get("ip").getS();
            // se o ip ja esta no map
            if (instancesComplexity.containsKey(ip)){
                // soma o número de requests que foram feitos
                int numReq = instancesComplexity.get(ip);
                instancesComplexity.put(ip, numReq+1);
            } else { //se o ip nao esta no mapa, adiciona
                instancesComplexity.put(ip, 0);
            }
        }

        // compares num of requests and chooses best worker
        Integer minValue = null;
        for (Map.Entry<String, Integer> entry : instancesComplexity.entrySet()) {
            Integer value = entry.getValue();
            if (minValue == null || value < minValue) {
                minValue = value;
                bestWorkerIp = entry.getKey();
            }
        }
        return bestWorkerIp;
    }

    private double getAvgNumExecutedInstructions(String requestType){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            if (itemMetrics.get("requestType").getS().equals(requestType)){
                double num = Double.valueOf((itemMetrics.get("numExecutedInstructions")).getN());
                sum = sum + num;
                i += 1;
            }
        }
        if (i == 0){
            return 0;
        }
        return sum/i;
    }

    private double getAvgNumExecutedBB(String requestType){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            if (itemMetrics.get("requestType").getS().equals(requestType)){
                double num = Double.valueOf((itemMetrics.get("numExecutedBB")).getN());
                sum = sum + num;
                i += 1;
            }
        }
        if (i == 0){
            return 0;
        }
        return sum/i;
    }

    private double getAvgNumExecutedMethods(String requestType){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            if (itemMetrics.get("requestType").getS().equals(requestType)){
                double num = Double.valueOf((itemMetrics.get("numExecutedMethods")).getN());
                sum = sum + num;
                i += 1;
            }
        }
        if (i == 0){
            return 0;
        }
        return sum/i;
    }

    private int getThreshold() {
        //TODO: Define a reasonable threshold
        return 0;
    }

    private String invokeLambda(String requestType) {
        // TODO: Change payload and lambda function name
        InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("myLambdaFunction").withPayload("{ \"key\": \"value\" }");
        InvokeResult invokeResult = lambda.invoke(invokeRequest);
        String response = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
        System.out.println("Lambda response: " + response);
        return response;
    }

    private String forwardToWorker(String requestType, String workerIP) {
        // TODO: Implement VM forwarding logic

        String WORKER_IP = workerIP.replace("-", ".");;
        String WORKER_PORT = "8000";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + WORKER_IP + ":" + WORKER_PORT + "/" + requestType))
            .POST(HttpRequest.BodyPublishers.ofString(requestType))
            .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error occurred while forwarding request to worker: " + e.getMessage();
        }
    }

    public void getDynamoDBMetrics(){
        // Calculate the timestamp for 5 minutes ago
        Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5));

        // Convert Instant to ZonedDateTime with a specific time zone
        ZonedDateTime zonedDateTime = fiveMinutesAgo.atZone(ZoneId.of("UTC"));

        // Format the timestamp as a string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String fiveMinutesAgoStr = formatter.format(zonedDateTime);

        // Construct the filter expression
        String filterExpression = "#time >= :timeVal";
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":timeVal", new AttributeValue().withS(fiveMinutesAgoStr));

        // Scan items with the specified filter expression
        ScanRequest scanRequest = new ScanRequest()
                .withTableName("MetricsTable")
                .withFilterExpression(filterExpression)
                .withExpressionAttributeNames(Collections.singletonMap("#time", "time"))
                .withExpressionAttributeValues(expressionAttributeValues);

        ScanResult result = dynamoDB.scan(scanRequest);
        dynamoDBMetrics = result.getItems();
        System.out.println("Got metrics from DB:\n" + dynamoDBMetrics);
    }
}
