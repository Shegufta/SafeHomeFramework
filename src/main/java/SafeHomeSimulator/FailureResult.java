/**
 * (Deprecated) Failure Result for SafeHome.
 *
 * FailureResult collects results when running SafeHome with failure cases.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 14-Aug-19
 * @time 12:13 AM
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


public class FailureResult
{
    public float abortedRtnCnt;
    public float totalOnTheFlyCmdCount;
    public float totalFailureRecoveryCommandSent;

    public FailureResult(
            float _abortedRtnCnt,
            float _totalFailureRecoveryCommandSent,
            float _totalOnTheFlyCmdCount
    )
    {
        this.abortedRtnCnt = _abortedRtnCnt;
        this.totalFailureRecoveryCommandSent = _totalFailureRecoveryCommandSent;
        this.totalOnTheFlyCmdCount = _totalOnTheFlyCmdCount;
    }

    public float getAbtRtnVsTotalRtnRatio(float totalRoutine)
    {
        if(totalRoutine == 0.0f)
            return 0.0f;

        return this.abortedRtnCnt / totalRoutine;
    }

    public float getRecoveryCmdSentRatio()
    {
        if(this.totalOnTheFlyCmdCount == 0.0f)
            return 0.0f;

        return this.totalFailureRecoveryCommandSent/this.totalOnTheFlyCmdCount;
    }
}
