package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import java.util.Map;
import java.util.HashMap;
import com.amazonaws.*;


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
        // TODO: Get the instances from AWS since they are already Initialized there
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().build();
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().build();
        AWSLambda lambda = AWSLambdaClientBuilder.standard().build();
        AmazonAutoScaling autoScaling = AmazonAutoScalingClientBuilder.standard().build();

        Map<String, Integer> metricsCache = new HashMap<>();
        LoadBalancer loadBalancer = new LoadBalancer(dynamoDB, ec2, lambda, metricsCache);
        AutoScaler autoScaler = new AutoScaler(autoScaling, "your-asg-name"); // Replace with your ASG name

        LoadBalancerServer server = new LoadBalancerServer(loadBalancer, autoScaler);
        server.start();
    }
}
