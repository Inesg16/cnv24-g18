package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.Map;
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

            String requestLine = in.readLine();
            System.out.println("Recieved request: " + requestLine);
            // Split the request line by spaces
            String[] parts = requestLine.split("\\s+");
            String requestType = "";

            // Extract the requestType from the request line (blurimage, raytracer, enhanceimage)
            if (parts.length >= 2) {
                requestType = parts[1].substring(1);
            } else {
                out.println("There was a formating error while processing your request");
            }

            String result = loadBalancer.handleRequest(requestType);

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
