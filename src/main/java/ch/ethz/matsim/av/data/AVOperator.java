package ch.ethz.matsim.av.data;

import ch.ethz.matsim.av.config.AVOperatorConfig;
import org.matsim.api.core.v01.Id;

public interface AVOperator {
    Id<AVOperator> getId();
    AVOperatorConfig getConfig();
}
