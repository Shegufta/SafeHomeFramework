/**
 * ExpResults for SafeHome
 *
 * ExpResults stores the metric data for further data analytics. It is used
 * to store the metric that are measured during runtime. It also includes an
 * object for Measurement for other metrics.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Aug-19
 * @time 1:05 AM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package SafeHomeSimulator;

import java.util.HashMap;
import java.util.Map;


public class ExpResults
{
    public Map<Float, Integer> waitTimeHistogram;
    public Map<Float, Integer> e2eTimeHistogram;
    public Map<Float, Integer> back2backRtnExectnTimeHistogram;
    public Map<Float, Integer> latencyOverheadHistogram;
    public Map<Float, Integer> e2eVsWaitTimeHistogram;
    public Map<Float, Integer> stretchRatioHistogram;
    public Map<Float, Integer> EV_executionLatencyHistogram = null;

    public Map<Float, Integer> abortHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    public Map<Float, Integer> totalRollbackHistogramGSVPSV = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    public Map<Float, Integer> perRtnRollbackCmdHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
    //public Map<Float, Integer> onTheFlyHistogram = null; // new these variables from inside FailureAnalyzer.simulateFailure();
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
