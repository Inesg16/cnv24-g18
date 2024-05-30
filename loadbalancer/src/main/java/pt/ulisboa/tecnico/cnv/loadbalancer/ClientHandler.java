package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Base64;
import java.util.HashMap;
import com.amazonaws.*;
import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;



public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private LoadBalancer loadBalancer;
    private AutoScaler autoScaler;

    public ClientHandler(Socket socket, LoadBalancer loadBalancer, AutoScaler autoScaler) {
        this.clientSocket = socket;
        this.loadBalancer = loadBalancer;
        this.autoScaler = autoScaler;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read the request line
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                out.println("Invalid request: Request line is empty.");
                return;
            }
            System.out.println("Received request: " + requestLine);

            // Split the request line by spaces
            String[] parts = requestLine.split("\\s+");
            String requestType = "";

            // Extract the requestType from the request line (blurimage, raytracer, enhanceimage)
            if (parts.length >= 2) {
                requestType = parts[1].substring(1);
            } else {
                out.println("There was a formatting error while processing your request.");
                return;
            }

            // Read the headers
            String headerLine;
            int contentLength = 0;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("Content-Length:")) {
                    try {
                        contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                    } catch (NumberFormatException e) {
                        out.println("Invalid Content-Length header.");
                        return;
                    }
                }
            }

            // Ensure Content-Length is valid
            if (contentLength <= 0) {
                out.println("Invalid Content-Length header.");
                return;
            }

            // Read the request body (payload)
            char[] body = new char[contentLength];
            int readChars = in.read(body, 0, contentLength);
            if (readChars != contentLength) {
                out.println("Error reading request body.");
                return;
            }
            String requestPayload = new String(body);
            byte[] decoded = Base64.getDecoder().decode(requestPayload);
            System.out.println("Received payload: " + requestPayload);

            // Process the request
            String result = loadBalancer.handleRequest(requestType, requestPayload);

            // Send the response
            out.println(result);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
