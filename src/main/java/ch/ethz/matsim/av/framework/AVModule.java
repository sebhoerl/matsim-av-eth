package ch.ethz.matsim.av.framework;

import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.config.AVConfigReader;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.config.AVOperatorConfig;
import ch.ethz.matsim.av.data.*;
import ch.ethz.matsim.plcpc.ParallelLeastCostPathCalculator;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import ch.ethz.matsim.av.dispatcher.multi_od_heuristic.MultiODHeuristic;
import ch.ethz.matsim.av.dispatcher.single_fifo.SingleFIFODispatcher;
import ch.ethz.matsim.av.dispatcher.single_heuristic.SingleHeuristicDispatcher;
import ch.ethz.matsim.av.generator.AVGenerator;
import ch.ethz.matsim.av.generator.PopulationDensityGenerator;
import ch.ethz.matsim.av.replanning.AVOperatorChoiceStrategy;
import ch.ethz.matsim.av.routing.AVParallelRouterFactory;
import ch.ethz.matsim.av.routing.AVRoute;
import ch.ethz.matsim.av.routing.AVRouteFactory;
import ch.ethz.matsim.av.routing.AVRoutingModule;
import ch.ethz.matsim.av.scoring.AVScoringFunctionFactory;

import javax.inject.Named;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AVModule extends AbstractModule {
    final static public String AV_MODE = "av";
    final static Logger log = Logger.getLogger(AVModule.class);

	@Override
	public void install() {
        addRoutingModuleBinding(AV_MODE).to(AVRoutingModule.class);
        bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
        addControlerListenerBinding().to(AVLoader.class);

        bind(AVOperatorChoiceStrategy.class);
        addPlanStrategyBinding("AVOperatorChoice").to(AVOperatorChoiceStrategy.class);

        // Bind the AV travel time to the DVRP estimated travel time
        bind(TravelTime.class).annotatedWith(Names.named(AVModule.AV_MODE))
                .to(Key.get(TravelTime.class, Names.named(VrpTravelTimeModules.DVRP_ESTIMATED)));

        bind(VehicleType.class).annotatedWith(Names.named(AVModule.AV_MODE)).toInstance(VehicleUtils.getDefaultVehicleType());

        bind(AVOperatorFactory.class);
        bind(AVRouteFactory.class);

        configureDispatchmentStrategies();
        configureGeneratorStrategies();

        bind(AVParallelRouterFactory.class);
        addControlerListenerBinding().to(Key.get(ParallelLeastCostPathCalculator.class, Names.named(AVModule.AV_MODE)));
        addMobsimListenerBinding().to(Key.get(ParallelLeastCostPathCalculator.class, Names.named(AVModule.AV_MODE)));
	}

	@Provides
    @Singleton
    @Named(AVModule.AV_MODE)
	private ParallelLeastCostPathCalculator provideParallelLeastCostPathCalculator(AVConfigGroup config, AVParallelRouterFactory factory) {
        return new ParallelLeastCostPathCalculator((int) config.getParallelRouters(), factory);
    }

	private void configureDispatchmentStrategies() {
        bind(SingleFIFODispatcher.Factory.class);
        bind(SingleHeuristicDispatcher.Factory.class);
        bind(MultiODHeuristic.Factory.class);

        AVUtils.bindDispatcherFactory(binder(), "SingleFIFO").to(SingleFIFODispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), "SingleHeuristic").to(SingleHeuristicDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), "MultiOD").to(MultiODHeuristic.Factory.class);
    }

    private void configureGeneratorStrategies() {
        bind(PopulationDensityGenerator.Factory.class);
        AVUtils.bindGeneratorFactory(binder(), "PopulationDensity").to(PopulationDensityGenerator.Factory.class);
    }

    @Provides
    RouteFactories provideRouteFactories(AVRouteFactory routeFactory) {
        RouteFactories factories = new RouteFactories();
        factories.setRouteFactory(AVRoute.class, routeFactory);
        return factories;
    }

	@Provides @Named(AVModule.AV_MODE)
    LeastCostPathCalculator provideLeastCostPathCalculator(Network network, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime) {
        return new Dijkstra(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }

	@Provides @Singleton
    Map<Id<AVOperator>, AVOperator> provideOperators(AVConfig config, AVOperatorFactory factory) {
        Map<Id<AVOperator>, AVOperator> operators = new HashMap<>();

        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            operators.put(oc.getId(), factory.createOperator(oc.getId(), oc));
        }

        return operators;
    }

    @Provides @Singleton
    AVConfig provideAVConfig(Config config, AVConfigGroup configGroup) {
        File basePath = new File(config.getContext().getPath()).getParentFile();
        File configPath = new File(basePath, configGroup.getConfigPath());

        AVConfig avConfig = new AVConfig();
        AVConfigReader reader = new AVConfigReader(avConfig);

        reader.readFile(configPath.getAbsolutePath());
        return avConfig;
    }

    @Provides @Singleton
    public AVData provideData(Map<Id<AVOperator>, AVOperator> operators, Map<Id<AVOperator>, List<AVVehicle>> vehicles) {
        AVData data = new AVData();

        for (List<AVVehicle> vehs : vehicles.values()) {
            for (AVVehicle vehicle : vehs) {
                data.addVehicle(vehicle);
            }
        }

        return data;
    }

    @Provides @Singleton
    Map<Id<AVOperator>, AVGenerator> provideGenerators(Map<String, AVGenerator.AVGeneratorFactory> factories, AVConfig config) {
        Map<Id<AVOperator>, AVGenerator> generators = new HashMap<>();

        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            AVGeneratorConfig gc = oc.getGeneratorConfig();
            String strategy = gc.getStrategyName();

            if (!factories.containsKey(strategy)) {
                throw new IllegalArgumentException("Generator strategy '" + strategy + "' is not registered.");
            }

            AVGenerator.AVGeneratorFactory factory = factories.get(strategy);
            AVGenerator generator = factory.createGenerator(gc);

            generators.put(oc.getId(), generator);
        }

        return generators;
    }

    @Provides @Singleton
    public Map<Id<AVOperator>, List<AVVehicle>> provideVehicles(Map<Id<AVOperator>, AVOperator> operators, Map<Id<AVOperator>, AVGenerator> generators) {
        Map<Id<AVOperator>, List<AVVehicle>> vehicles = new HashMap<>();

        for (AVOperator operator : operators.values()) {
            LinkedList<AVVehicle> operatorList = new LinkedList<>();

            AVGenerator generator = generators.get(operator.getId());

            while (generator.hasNext()) {
                AVVehicle vehicle = generator.next();
                vehicle.setOpeartor(operator);
                operatorList.add(vehicle);
            }

            vehicles.put(operator.getId(), operatorList);
        }

        return vehicles;
    }
}
