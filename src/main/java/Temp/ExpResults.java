package Temp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Aug-19
 * @time 1:05 AM
 */
public class ExpResults
{
    public Map<Float, Float> waitTimeHistogram;
    public Map<Float, Float> latencyOverheadHistogram;
    public Map<Float, Float> stretchRatioHistogram;

    public FailureAnalyzer failureAnalyzer = null;
    public Measurement measurement;


    public ExpResults()
    {

        this.waitTimeHistogram = new HashMap<>();
        this.latencyOverheadHistogram = new HashMap<>();
        this.stretchRatioHistogram = new HashMap<>();
    }
}
