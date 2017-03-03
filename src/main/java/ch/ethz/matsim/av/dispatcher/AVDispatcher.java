package ch.ethz.matsim.av.dispatcher;

import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.data.AVVehicle;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.schedule.AVTask;

public interface AVDispatcher {
    void onRequestSubmitted(AVRequest request);
    void onNextTaskStarted(AVTask task);
    void onNextTimestep(double now);

    void addVehicle(AVVehicle vehicle);

    interface AVDispatcherFactory {
        AVDispatcher createDispatcher(AVDispatcherConfig config);
    }
}
