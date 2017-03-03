package ch.ethz.matsim.av.vrpagent;

import com.google.inject.Inject;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import ch.ethz.matsim.av.passenger.AVPassengerDropoffActivity;
import ch.ethz.matsim.av.passenger.AVPassengerPickupActivity;
import ch.ethz.matsim.av.schedule.AVDropoffTask;
import ch.ethz.matsim.av.schedule.AVPickupTask;
import ch.ethz.matsim.av.schedule.AVTask;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;

import javax.inject.Named;

public class AVActionCreator implements VrpAgentLogic.DynActionCreator {
    public static final String PICKUP_ACTIVITY_TYPE = "AVPickup";
    public static final String DROPOFF_ACTIVITY_TYPE = "AVDropoff";
    public static final String STAY_ACTIVITY_TYPE = "AVStay";

    @Inject
    private PassengerEngine passengerEngine;

    @Inject
    private VrpLegs.LegCreator legCreator;

    @Inject @Named("pickupDuration")
    private Double pickupDuration;

    @Override
    public DynAction createAction(DynAgent dynAgent, final Task task, double now)
    {
    	if (task instanceof AVTask) {
    		switch (((AVTask) task).getAVTaskType()) {
    			case PICKUP:
    				AVPickupTask mpt = (AVPickupTask) task;
    	    		return new AVPassengerPickupActivity(passengerEngine, dynAgent, mpt, mpt.getRequests(),
    	                    pickupDuration, PICKUP_ACTIVITY_TYPE);
                case DROPOFF:
    				AVDropoffTask mdt = (AVDropoffTask) task;
    				return new AVPassengerDropoffActivity(passengerEngine, dynAgent, mdt, mdt.getRequests(),
                            DROPOFF_ACTIVITY_TYPE);
                case DRIVE:
    				return legCreator.createLeg((DriveTask)task);
                case STAY:
                    return new VrpActivity(STAY_ACTIVITY_TYPE, (StayTask) task);
    	    	default:
    	    		throw new IllegalStateException();
    		}
    	} else {
    		throw new IllegalArgumentException();
        }
    }
}
