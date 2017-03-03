package ch.ethz.matsim.av.dispatcher.multi_od_heuristic.aggregation;

import ch.ethz.matsim.av.data.AVVehicle;
import ch.ethz.matsim.av.dispatcher.multi_od_heuristic.TravelTimeEstimator;
import ch.ethz.matsim.av.passenger.AVRequest;

import java.util.Collection;
import java.util.LinkedList;

public class AggregatedRequest {
    final private AVRequest master;
    final Collection<AVRequest> slaves = new LinkedList<>();

    final long occupancyThreshold = 4;
    final double distanceThreshold;

    private AVVehicle vehicle = null;

    final private TravelTimeEstimator estimator;

    public AggregatedRequest(AVRequest master, TravelTimeEstimator estimator) {
        this.master = master;
        this.estimator = estimator;

        distanceThreshold = estimator.getThreshold();
    }

    public AVRequest getMasterRequest() {
        return master;
    }

    public void addSlaveRequest(AVRequest slave) {
        slaves.add(slave);
    }

    public Collection<AVRequest> getSlaveRequests() {
        return slaves;
    }

    public Double accept(AVRequest candidate) {
        if (slaves.size() >= occupancyThreshold - 1) {
            return null;
        }

        double distance1 = estimator.getDistance(candidate.getToLink(), master.getToLink(), master.getT0());

        if (distance1 > distanceThreshold) {
            return null;
        }

        double distance2 = estimator.getDistance(candidate.getFromLink(), master.getFromLink(), master.getT0());

        if (distance2 > distanceThreshold) {
            return null;
        }

        return distance1 + distance2;
    }

    public void setVehicle(AVVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public AVVehicle getVehicle() {
        return vehicle;
    }
}
