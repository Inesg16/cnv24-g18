package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.cloudwatch.*;
import com.amazonaws.services.cloudwatch.model.*;

import java.util.*;

public class AutoScaler {

    private AmazonAutoScaling autoScaling;
    private String autoScalingGroupName;
    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;

    public AutoScaler(AmazonAutoScaling autoScaling, String autoScalingGroupName, AmazonEC2 ec2, AmazonCloudWatch cloudWatch) {
        this.autoScaling = autoScaling;
        this.autoScalingGroupName = autoScalingGroupName;
        this.ec2 = ec2;
        this.cloudWatch = cloudWatch;
    }

    public String getAutoScalingGroupName() {
        return this.autoScalingGroupName;
    }

    public void adjustAutoScalingGroup() {
        double cpuUsage = getAverageCPUUtilization();
        int currentCapacity = getCurrentCapacity();

        if (cpuUsage > getHighLoadThreshold()) {
            increaseASGCapacity(currentCapacity + 1);
        } else if (cpuUsage < getLowLoadThreshold()) {
            decreaseASGCapacity(currentCapacity - 1);
        }
    }

    private double getAverageCPUUtilization() {
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withNamespace("AWS/EC2")
                .withMetricName("CPUUtilization")
                .withPeriod(300)
                .withStatistics("Average")
                .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(autoScalingGroupName))
                .withStartTime(new Date(System.currentTimeMillis() - 3600000))  // past hour
                .withEndTime(new Date());

        GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
        List<Datapoint> datapoints = result.getDatapoints();

        double sum = 0;
        for (Datapoint dp : datapoints) {
            sum += dp.getAverage();
        }
        return sum / datapoints.size();
    }

    private int getCurrentCapacity() {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(autoScalingGroupName);
        DescribeAutoScalingGroupsResult result = autoScaling.describeAutoScalingGroups(request);
        AutoScalingGroup asg = result.getAutoScalingGroups().get(0);
        return asg.getDesiredCapacity();
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
        return 80;
    }

    private int getLowLoadThreshold() {
        return 20;
    }

    public List<String> getActiveInstanceIPs() {
        List<String> instanceIPs = new ArrayList<>();
        try {
            DescribeAutoScalingGroupsRequest asgRequest = new DescribeAutoScalingGroupsRequest()
                    .withAutoScalingGroupNames(autoScalingGroupName);
            DescribeAutoScalingGroupsResult asgResult = autoScaling.describeAutoScalingGroups(asgRequest);
            List<AutoScalingGroup> asgList = asgResult.getAutoScalingGroups();

            for (AutoScalingGroup asg : asgList) {
                List<String> instanceIds = new ArrayList<>();
                for (com.amazonaws.services.autoscaling.model.Instance instance : asg.getInstances()) {
                    instanceIds.add(instance.getInstanceId());
                }

                DescribeInstancesRequest ec2Request = new DescribeInstancesRequest().withInstanceIds(instanceIds);
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
