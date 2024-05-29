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
import java.util.HashMap;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            return forwardToWorker(requestType);
        }
    }

    private double estimateComplexity(String requestType) {
        
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

        double avgNumExecutedInstructions = getAvgNumExecutedInstructions();
        double avgNumExecutedBB = getAvgNumExecutedBB();
        double avgNumExecutedMethods = getAvgNumExecutedMethods();

        complexity = reqWeight * (w1*avgNumExecutedInstructions + w2*avgNumExecutedBB + w3*avgNumExecutedMethods);

        return complexity;
    }

    private double getAvgNumExecutedInstructions(){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            double num = Double.valueOf((itemMetrics.get("numExecutedInstructions")).getN());
            sum = sum + num;
            i += 1;
        }
        if (i == 0){
            return 0;
        }
        return sum/i;
    }

    private double getAvgNumExecutedBB(){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            double num = Double.valueOf((itemMetrics.get("numExecutedBB")).getN());
            sum = sum + num;
            i += 1;
        }
        if (i == 0){
            return 0;
        }
        return sum/i;
    }

    private double getAvgNumExecutedMethods(){
        double sum = 0;
        int i = 0;
        for (Map<String, AttributeValue> itemMetrics : dynamoDBMetrics) {
            double num = Double.valueOf((itemMetrics.get("numExecutedMethods")).getN());
            sum = sum + num;
            i += 1;
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

    private String forwardToWorker(String requestType) {
        // TODO: Implement VM forwarding logic

        activeWorkerIPs = autoScaler.getActiveInstanceIPs();
        System.out.println(activeWorkerIPs);

        // TODO: Based on the metrics fetched from each of the available machines, estimate the weight of the request
        // TODO: On a tie apply a load balancing algorythm (i.e: round robbin, leat connection)

        return "VM response";
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
