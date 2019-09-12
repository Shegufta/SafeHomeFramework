package Temp;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 14-Aug-19
 * @time 12:13 AM
 */
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
