package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.Map;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.*;


public class LoadBalancer {

    private AmazonDynamoDB dynamoDB;
    private AmazonEC2 ec2;
    private AWSLambda lambda;
    private Map<String, Integer> metricsCache;

    public LoadBalancer(AmazonDynamoDB dynamoDB, AmazonEC2 ec2, AWSLambda lambda, Map<String, Integer> metricsCache) {
        this.dynamoDB = dynamoDB;
        this.ec2 = ec2;
        this.lambda = lambda;
        this.metricsCache = metricsCache;
    }

    public String handleRequest(String requestType) {
        int complexity = estimateComplexity(requestType);

        // TODO: Change the way the loadbalancer distributes the requests
        if (complexity < getThreshold()) {
            return invokeLambda(requestType);
        } else {
            return forwardToVM(requestType);
        }
    }

    private int estimateComplexity(String requestType) {
        return metricsCache.getOrDefault(requestType, 0);
    }

    private int getThreshold() {
        return 50;
    }

    private String invokeLambda(String requestType) {
        // TODO: Implement Lambda invocation logic
        return "Lambda response";
    }

    private String forwardToVM(String requestType) {
        // TODO: Implement VM forwarding logic
        return "VM response";
    }
}
