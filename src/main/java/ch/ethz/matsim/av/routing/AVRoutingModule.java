package ch.ethz.matsim.av.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;
import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.replanning.AVOperatorChoiceStrategy;

import java.util.Collections;
import java.util.List;

@Singleton
public class AVRoutingModule implements RoutingModule {
    @Inject private AVOperatorChoiceStrategy choiceStrategy;
    @Inject private AVRouteFactory routeFactory;

    @Override
    public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
        Id<AVOperator> operator = choiceStrategy.chooseRandomOperator();

        AVRoute route = routeFactory.createRoute(fromFacility.getLinkId(), toFacility.getLinkId(), operator);
        route.setDistance(Double.NaN);
        route.setTravelTime(Double.NaN);

        Leg leg = PopulationUtils.createLeg(AVModule.AV_MODE);
        leg.setDepartureTime(departureTime);
        leg.setTravelTime(Double.NaN);
        leg.setRoute(route);

        return Collections.singletonList(leg);
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return EmptyStageActivityTypes.INSTANCE;
    }
}
