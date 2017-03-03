package ch.ethz.matsim.plcpc;

public interface LeastCostPathCalculatorWorker extends Runnable {
    void addTask(ParallelLeastCostPathCalculatorTask task);
}
