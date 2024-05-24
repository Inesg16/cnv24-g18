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

            String requestType = in.readLine();
            String result = loadBalancer.handleRequest(requestType);

            out.println(result);

            // Auto-scaling logic
            int currentLoad = getCurrentLoad();
            int desiredCapacity = getDesiredCapacity();
            autoScaler.adjustAutoScalingGroup(currentLoad, desiredCapacity);

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

    private int getCurrentLoad() {
        // TODO: Implement logic to get current load
        return 0;
    }

    private int getDesiredCapacity() {
        // Retrieve current desired capacity of the ASG
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames("your-asg-name"); // Replace with your ASG name
        DescribeAutoScalingGroupsResult result = autoScaler.describeAutoScalingGroups(request);
        return result.getAutoScalingGroups().get(0).getDesiredCapacity();
    }
}
