package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import java.util.ArrayList;
import java.util.List;

public class AutoScaler {

    private AmazonAutoScaling autoScaling;
    private String autoScalingGroupName;
    private AmazonEC2 ec2;

    public AutoScaler(AmazonAutoScaling autoScaling, String autoScalingGroupName, AmazonEC2 ec2) {
        this.autoScaling = autoScaling;
        this.autoScalingGroupName = autoScalingGroupName;
        this.ec2 = ec2;
    }

    public String getAutoScalingGroupName(){
        return this.autoScalingGroupName;
    }

    public void adjustAutoScalingGroup(int currentLoad, int desiredCapacity) {
        // TODO: Change the way it gets the threshold so it uses our metrics from the dynamoDB
        if (currentLoad > getHighLoadThreshold()) {
            increaseASGCapacity(desiredCapacity + 1);
        } else if (currentLoad < getLowLoadThreshold()) {
            decreaseASGCapacity(desiredCapacity - 1);
        }
    }

    // Method to describe Auto Scaling groups
    public DescribeAutoScalingGroupsResult describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return autoScaling.describeAutoScalingGroups(request);
    }

    private void increaseASGCapacity(int newCapacity) {
        SetDesiredCapacityRequest request = new SetDesiredCapacityRequest()
            .withAutoScalingGroupName(autoScalingGroupName)
            .withDesiredCapacity(newCapacity);
        autoScaling.setDesiredCapacity(request);
    }

    private void decreaseASGCapacity(int newCapacity) {
        SetDesiredCapacityRequest request = new SetDesiredCapacityRequest()
            .withAutoScalingGroupName(autoScalingGroupName)
            .withDesiredCapacity(newCapacity);
        autoScaling.setDesiredCapacity(request);
    }

    private int getHighLoadThreshold() {
        // TODO: Change the way it gets the threshold so it uses our metrics from the dynamoDB
        return 80; // Example threshold value
    }

    private int getLowLoadThreshold() {
        // TODO: Change the way it gets the threshold so it uses our metrics from the dynamoDB
        return 20; // Example threshold value
    }

    public List<String> getActiveInstanceIPs() {
        String groupName = autoScalingGroupName;
        List<String> instanceIPs = new ArrayList<>();
        try {
            DescribeAutoScalingGroupsRequest asgRequest = new DescribeAutoScalingGroupsRequest()
                    .withAutoScalingGroupNames(groupName);
            DescribeAutoScalingGroupsResult asgResult = autoScaling.describeAutoScalingGroups(asgRequest);
            List<AutoScalingGroup> asgList = asgResult.getAutoScalingGroups();

            for (AutoScalingGroup asg : asgList) {
                List<String> instanceIds = new ArrayList<>();
                for (com.amazonaws.services.autoscaling.model.Instance instance : asg.getInstances()) {
                    instanceIds.add(instance.getInstanceId());
                }

                DescribeInstancesRequest ec2Request = new DescribeInstancesRequest()
                        .withInstanceIds(instanceIds);
                DescribeInstancesResult ec2Result = ec2.describeInstances(ec2Request);

                for (Reservation reservation : ec2Result.getReservations()) {
                    for (com.amazonaws.services.ec2.model.Instance ec2Instance : reservation.getInstances()) {
                        instanceIPs.add(ec2Instance.getPublicIpAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instanceIPs;
    }
}
