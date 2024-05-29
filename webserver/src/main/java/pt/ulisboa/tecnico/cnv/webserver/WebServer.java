package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.util.TableUtils.TableNeverTransitionedToStateException;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.util.*;

public class WebServer {

    private static String AWS_REGION = "eu-north-1";
    private static AmazonDynamoDB dynamoDB;
    private static final String METRICS_FILE_PATH = "icount-metrics.out";
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        try {
            // DynamoDB
            initializeDynamoDB();
            String tableName = "MetricsTable";
            createTable(dynamoDB, tableName);

            // Creates a scheduler for the metrics
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> WebServer.sendMetricsToDatabase(), 0, 30, TimeUnit.SECONDS);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }

        // HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new RootHandler());
        server.createContext("/raytracer", new RaytracerHandler(dynamoDB));
        server.createContext("/blurimage", new BlurImageHandler(dynamoDB));
        server.createContext("/enhanceimage", new EnhanceImageHandler(dynamoDB));
        server.start();

    }

    private static String getInstanceIP() throws UnknownHostException{
        try {
            return InetAddress.getLocalHost().getHostAddress().replace(".", "-");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get instance IP", e);
        }
    }

    private static void initializeDynamoDB() {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();
    }

    private static void createTable(AmazonDynamoDB dynamoDB, String tableName) throws TableNeverTransitionedToStateException, InterruptedException {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
        .withKeySchema(new KeySchemaElement().withAttributeName("ip").withKeyType(KeyType.HASH))
        .withAttributeDefinitions(new AttributeDefinition().withAttributeName("ip").withAttributeType(ScalarAttributeType.S))
        .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
        // wait for the table to move into ACTIVE state
        TableUtils.waitUntilActive(dynamoDB, tableName);

        // Describe our new table
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: " + tableDescription);
    }

     private static void sendMetricsToDatabase() {
        String filename = "icount-metrics.out";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                dynamoDB.putItem(new PutItemRequest("MetricsTable", parseLine(line)));
            }
            reader.close();

            // Delete all lines from the file
            PrintWriter writer = new PrintWriter(filename);
            writer.print(""); // Writing an empty string to the file
            writer.close();
            System.out.println("Sent metrics to DynamoDB");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, AttributeValue> parseLine(String line) throws UnknownHostException {
        String ip = getInstanceIP(); // ip as xx-xx-xx-xx
        String[] parts = line.split("\\|");
        long threadID = Long.parseLong(parts[0]);
        String time = parts[1];
        String requestType = parts[2];
        long numExecutedMethods = Long.parseLong(parts[3]);
        long numExecutedBB = Long.parseLong(parts[4]);
        long numExecutedInstructions = Long.parseLong(parts[5]);

        return newItem(ip, threadID, time, requestType, numExecutedMethods, numExecutedBB, numExecutedInstructions);
    }

    private static Map<String, AttributeValue> newItem(String ip, long threadID, String time, String requestType, long numExecutedMethods, long numExecutedBB, long numExecutedInstructions) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("ip", new AttributeValue(ip));
        item.put("threadID", new AttributeValue().withN(Long.toString(threadID)));
        item.put("time", new AttributeValue(time));
        item.put("requestType", new AttributeValue(requestType));
        item.put("numExecutedMethods", new AttributeValue().withN(Long.toString(numExecutedMethods)));
        item.put("numExecutedBB", new AttributeValue().withN(Long.toString(numExecutedBB)));
        item.put("numExecutedInstructions", new AttributeValue().withN(Long.toString(numExecutedInstructions)));
        return item;
    }
}
