package ch.ethz.matsim.plcpc;

import org.matsim.core.router.util.LeastCostPathCalculator;

public interface ParallelLeastCostPathCalculatorFactory {
    LeastCostPathCalculator createRouter();
}
