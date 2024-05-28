package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.Map;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private AmazonDynamoDB dynamoDB;
    private AmazonEC2 ec2;
    private AWSLambda lambda;
    private AutoScaler autoScaler;
    private List<String> activeWorkerIPs;


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

        return "VM response";
    }
}
