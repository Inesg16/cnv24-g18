package db.src.main.java.pt.ulisboa.tecnico.cnv.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class AmazonDynamoDBSample {

    private static String AWS_REGION = "us-east-2";
    private static AmazonDynamoDB dynamoDB;

    public static void main(String[] args) throws Exception {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        try {
            String instanceIP = getInstanceIP();
            String tableName = "Instance_" + instanceIP;

            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("ThreadID").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("ThreadID").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            TableUtils.waitUntilActive(dynamoDB, tableName);

            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            System.out.println("Table Description: " + dynamoDB.describeTable(describeTableRequest).getTable());

            dynamoDB.putItem(new PutItemRequest(tableName, newItem("Thread-1", "2023-05-25T10:00:00Z", "GET", 10, 5, 1000)));
            dynamoDB.putItem(new PutItemRequest(tableName, newItem("Thread-2", "2023-05-25T10:05:00Z", "POST", 15, 8, 2000)));

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered a serious internal problem while trying to communicate with AWS, such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    private static String getInstanceIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress().replace(".", "-");
    }

    private static Map<String, AttributeValue> newItem(String threadID, String time, String requestType, int numExecutedMethods, int numExecutedBB, int numExecutedInstructions) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("ThreadID", new AttributeValue(threadID));
        item.put("Time", new AttributeValue(time));
        item.put("RequestType", new AttributeValue(requestType));
        item.put("NumExecutedMethods", new AttributeValue().withN(Integer.toString(numExecutedMethods)));
        item.put("NumExecutedBB", new AttributeValue().withN(Integer.toString(numExecutedBB)));
        item.put("NumExecutedInstructions", new AttributeValue().withN(Integer.toString(numExecutedInstructions)));
        return item;
    }
}
