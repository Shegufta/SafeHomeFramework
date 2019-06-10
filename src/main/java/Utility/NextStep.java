package Utility;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/9/2019
 * @time 5:27 PM
 */
public class NextStep
{
    public static final int HALT = -1;

    public int nextCommandIndex;
    public int waitTimeMilliSecond;
    public int routineID;

    public NextStep(int routineID, int nextCommandIndex, int waitTimeMilliSecond)
    {
        this.routineID = routineID;
        this.nextCommandIndex = nextCommandIndex;
        this.waitTimeMilliSecond = waitTimeMilliSecond;

        if(0 < this.waitTimeMilliSecond)
            assert(this.nextCommandIndex == HALT);

        if(this.nextCommandIndex == HALT)
            assert(0 < this.waitTimeMilliSecond);
    }
}
