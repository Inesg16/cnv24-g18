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
        int complexity = estimateComplexity(requestType);

        // TODO: Change the way the loadbalancer distributes the requests
        if (complexity < getThreshold()) {
            return invokeLambda(requestType);
        } else {
            return forwardToWorker(requestType);
        }
    }

    private int estimateComplexity(String requestType) {
        //TODO: Estimate complexity
        return 0;
    }

    private int getThreshold() {
        //TODO: Define a reasonable threshold
        return 50;
    }

    private String invokeLambda(String requestType) {
        // TODO: Change payload and lambda function name
        InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("myLambdaFunction").withPayload("{ \"key\": \"value\" }");
        InvokeResult invokeResult = lambda.invoke(invokeRequest);
        String response = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
        System.out.println("Lambda response: " + response);
        return response;
    }

    private String forwardToWorker(String requestType) {
        // TODO: Implement VM forwarding logic

        activeWorkerIPs = autoScaler.getActiveInstanceIPs();
        System.out.println(activeWorkerIPs);

        // TODO: Based on the metrics fetched from each of the available machines, estimate the weight of the request
        // TODO: On a tie apply a load balancing algorythm (i.e: round robbin, leat connection)
        String WORKER_IP = "";
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

        // Format the timestamp as a string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String fiveMinutesAgoStr = formatter.format(fiveMinutesAgo);

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
    }
}
