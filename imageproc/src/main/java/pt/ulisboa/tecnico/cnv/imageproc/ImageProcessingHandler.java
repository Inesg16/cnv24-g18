package pt.ulisboa.tecnico.cnv.imageproc;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.javassist.tools.ICount;


public abstract class ImageProcessingHandler implements HttpHandler, RequestHandler<Map<String,String>, String> {

    private AmazonDynamoDB dynamoDB;

    public ImageProcessingHandler() {
    }

    public ImageProcessingHandler(AmazonDynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    abstract BufferedImage process(BufferedImage bi) throws IOException;

    private String handleRequest(String inputEncoded, String format) {
        byte[] decoded = Base64.getDecoder().decode(inputEncoded);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            BufferedImage bi = ImageIO.read(bais);
            bi = process(bi);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, format, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return e.toString();
        }
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        // Handling CORS
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            t.sendResponseHeaders(204, -1);
            return;
        }

        InputStream stream = t.getRequestBody();
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        System.out.println("Received payload: " + result);  // Log the received payload
        String[] resultSplits = result.split(",");
        String format = resultSplits[0].split("/")[1].split(";")[0];

        String output = handleRequest(resultSplits[1], format);

        t.sendResponseHeaders(200, output.length());
        OutputStream os = t.getResponseBody();
        os.write(output.getBytes());
        os.close();

        // Log metrics
        ICount.printStatistics("imageproc");
    }


    private static Map<String, AttributeValue> newItem(long threadID, String time, String requestType, long numExecutedMethods, long numExecutedBB, long numExecutedInstructions) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("threadID", new AttributeValue().withN(Long.toString(threadID)));
        item.put("time", new AttributeValue(time));
        item.put("requestType", new AttributeValue(requestType));
        item.put("numExecutedMethods", new AttributeValue().withN(Long.toString(numExecutedMethods)));
        item.put("numExecutedBB", new AttributeValue().withN(Long.toString(numExecutedBB)));
        item.put("numExecutedInstructions", new AttributeValue().withN(Long.toString(numExecutedInstructions)));
        return item;
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        return handleRequest(event.get("body"), event.get("fileFormat"));
    }
}
