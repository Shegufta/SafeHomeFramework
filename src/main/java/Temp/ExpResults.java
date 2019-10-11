package Temp;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Aug-19
 * @time 1:05 AM
 */
public class ExpResults
{
    public Map<Float, Integer> waitTimeHistogram;
    public Map<Float, Integer> e2eTimeHistogram;
    public Map<Float, Integer> back2backRtnExectnTimeHistogram;
    public Map<Float, Integer> latencyOverheadHistogram;
    public Map<Float, Integer> e2eVsWaitTimeHistogram;
    public Map<Float, Integer> stretchRatioHistogram;

    public Map<Float, Integer> abortHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    public Map<Float, Integer> rollbackHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    public Map<Float, Integer> onTheFlyHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    //public Map<Float, Integer> executionLatencyHistogram = null;

    //public FailureAnalyzer failureAnalyzer = null;
    public Measurement measurement;


    public ExpResults()
    {

        this.waitTimeHistogram = new HashMap<>();
        this.e2eTimeHistogram = new HashMap<>();
        this.back2backRtnExectnTimeHistogram = new HashMap<>();
        this.latencyOverheadHistogram = new HashMap<>();
        this.e2eVsWaitTimeHistogram = new HashMap<>();
        this.stretchRatioHistogram = new HashMap<>();
    }
}
