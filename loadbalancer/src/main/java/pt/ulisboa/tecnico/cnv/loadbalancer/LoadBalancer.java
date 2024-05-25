package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.Map;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.*;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private AmazonDynamoDB dynamoDB;
    private AmazonEC2 ec2;
    private AWSLambda lambda;
    private AutoScaler autoScaler;

    public LoadBalancer(AmazonDynamoDB dynamoDB, AmazonEC2 ec2, AWSLambda lambda, AutoScaler autoScaler) {
        this.dynamoDB = dynamoDB;
        this.ec2 = ec2;
        this.lambda = lambda;
        this.autoScaler = autoScaler;
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
        return 50;
    }

    private String invokeLambda(String requestType) {
        // TODO: Implement Lambda invocation logic
        return "Lambda response";
    }

    private String forwardToWorker(String requestType) {
        // TODO: Implement VM forwarding logic
        // TODO: Round-Robin

        List<String> activeWorkerIPs = autoScaler.getActiveInstanceIPs();
        System.out.println(activeWorkerIPs);
        return "VM response";
    }
}
