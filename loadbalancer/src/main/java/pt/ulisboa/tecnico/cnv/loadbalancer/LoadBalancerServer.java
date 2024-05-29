package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.cloudwatch.*;

public class LoadBalancerServer {

    private ServerSocket serverSocket;
    private LoadBalancer loadBalancer;
    private AutoScaler autoScaler;
    private final int port = 8080;
    private ExecutorService threadPool;
    private static final String MYREGION = "eu-west-3";
    private static final String MYASGNAME = "cnv-autoscalinggroup";

    public LoadBalancerServer(LoadBalancer loadBalancer, AutoScaler autoScaler) {
        this.loadBalancer = loadBalancer;
        this.autoScaler = autoScaler;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Load Balancer is running on port " + port);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> autoScaler.adjustAutoScalingGroup(), 0, 5, TimeUnit.MINUTES);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(new ClientHandler(clientSocket, loadBalancer, autoScaler));
        }
    }

    public static void main(String[] args) throws IOException {
        AmazonAutoScaling autoScaling = createAutoScalingClient();
        AmazonDynamoDB dynamoDB = createDynamoDBClient();
        AmazonEC2 ec2 = createEC2Client();
        AWSLambda lambda = createLambdaClient();
        AmazonCloudWatch cloudWatch = createCloudWatchClient();

        AutoScaler autoScaler = new AutoScaler(autoScaling, MYASGNAME, ec2, cloudWatch);
        LoadBalancer loadBalancer = new LoadBalancer(dynamoDB, ec2, lambda, autoScaler);

        LoadBalancerServer server = new LoadBalancerServer(loadBalancer, autoScaler);
        server.start();
    }

    private static AmazonAutoScaling createAutoScalingClient() {
        return AmazonAutoScalingClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    private static AmazonDynamoDB createDynamoDBClient() {
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    private static AmazonEC2 createEC2Client() {
        return AmazonEC2ClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    private static AWSLambda createLambdaClient() {
        return AWSLambdaClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    private static AmazonCloudWatch createCloudWatchClient() {
        return AmazonCloudWatchClientBuilder.standard()
                .withRegion(MYREGION)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }
}
