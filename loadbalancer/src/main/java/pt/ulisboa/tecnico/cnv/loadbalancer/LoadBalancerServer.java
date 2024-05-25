package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoadBalancerServer {

    private ServerSocket serverSocket;
    private LoadBalancer loadBalancer;
    private AutoScaler autoScaler;
    private final int port = 8080;
    private ExecutorService threadPool;

    public LoadBalancerServer(LoadBalancer loadBalancer, AutoScaler autoScaler) {
        this.loadBalancer = loadBalancer;
        this.autoScaler = autoScaler;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Load Balancer is running on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(new ClientHandler(clientSocket, loadBalancer, autoScaler));
        }
    }

    public static void main(String[] args) throws IOException {
        // Initialize LoadBalancer and AutoScaler instances
        // TODO: Get the instances from AWS
        // Initialize AWS credentials
        String accessKey = "AKIA6ODU4A6TEKYVSNEF";
        String secretKey = "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";
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        String MYREGION = "eu-west-3";
        String MYASGNAME = "cnv-autoscalinggroup";

        // Initialize AWS service clients
        AmazonAutoScaling autoScaling = AmazonAutoScalingClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        AWSLambda lambda = AWSLambdaClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();


        AutoScaler autoScaler = new AutoScaler(autoScaling, MYASGNAME, ec2);

        LoadBalancer loadBalancer = new LoadBalancer(dynamoDB, ec2, lambda, autoScaler);

        LoadBalancerServer server = new LoadBalancerServer(loadBalancer, autoScaler);
        server.start();
    }
}
