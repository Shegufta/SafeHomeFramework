package Temp;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 14-Aug-19
 * @time 12:13 AM
 */
public class FailureResult
{
    public double abortedRtnCnt;
    public double totalOnTheFlyCmdCount;
    public double totalFailureRecoveryCommandSent;

    public FailureResult(
            double _abortedRtnCnt,
            double _totalFailureRecoveryCommandSent,
            double _totalOnTheFlyCmdCount
    )
    {
        this.abortedRtnCnt = _abortedRtnCnt;
        this.totalFailureRecoveryCommandSent = _totalFailureRecoveryCommandSent;
        this.totalOnTheFlyCmdCount = _totalOnTheFlyCmdCount;
    }

    public double getAbtRtnVsTotalRtnRatio(double totalRoutine)
    {
        if(totalRoutine == 0.0)
            return 0.0;

        return this.abortedRtnCnt / totalRoutine;
    }

    public double getRecoveryCmdSentRatio()
    {
        if(this.totalOnTheFlyCmdCount == 0.0)
            return 0.0;

        return this.totalFailureRecoveryCommandSent/this.totalOnTheFlyCmdCount;
    }
}
