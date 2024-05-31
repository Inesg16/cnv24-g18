package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ClientHandler implements HttpHandler {

    private String requestType;
    private LoadBalancer loadBalancer;
    private AutoScaler autoScaler;

    public ClientHandler(String requestType, LoadBalancer loadBalancer, AutoScaler autoScaler) {
        this.requestType = requestType;
        this.loadBalancer = loadBalancer;
        this.autoScaler = autoScaler;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        // Handling CORS
        System.out.println(this.requestType);
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

        String output = loadBalancer.handleRequest(this.requestType, result);
        output = String.format("data:image/%s;base64,%s", format, output);
        System.out.println("Got the following output:" + output);

        t.sendResponseHeaders(200, output.length());
        OutputStream os = t.getResponseBody();
        os.write(output.getBytes());
        os.close();
    }
}
