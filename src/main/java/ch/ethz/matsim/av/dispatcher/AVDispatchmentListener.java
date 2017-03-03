package ch.ethz.matsim.av.dispatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import ch.ethz.matsim.av.data.AVOperator;

import java.util.Map;

@Singleton
public class AVDispatchmentListener implements MobsimBeforeSimStepListener {
    @Inject
    Map<Id<AVOperator>, AVDispatcher> dispatchers;

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        for (AVDispatcher dispatcher : dispatchers.values()) {
            dispatcher.onNextTimestep(e.getSimulationTime());
        }
    }
}
