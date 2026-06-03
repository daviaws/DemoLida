package detectors;

import edu.memphis.ccrg.lida.pam.tasks.BasicDetectionAlgorithm;
import java.util.HashMap;
import java.util.Map;

public class LeafletReadyDetector extends BasicDetectionAlgorithm {

    private final Map<String, Object> params = new HashMap<>();

    @Override
    public void init() {
        super.init();
        params.put("mode", "leafletReady");
    }

    @Override
    public double detect() {
        Boolean ready = (Boolean) sensoryMemory.getSensoryContent("", params);
        return (ready != null && ready) ? 1.0 : 0.0;
    }
}