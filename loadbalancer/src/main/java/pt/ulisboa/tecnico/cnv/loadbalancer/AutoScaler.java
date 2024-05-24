package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.*;

public class AutoScaler {

    private AmazonAutoScaling autoScaling;
    private String autoScalingGroupName;

    public AutoScaler(AmazonAutoScaling autoScaling, String autoScalingGroupName) {
        this.autoScaling = autoScaling;
        this.autoScalingGroupName = autoScalingGroupName;
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
}
