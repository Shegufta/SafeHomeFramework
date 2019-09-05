package Temp;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:03 AM
 */
public class Command
{
    public DEV_ID devID;
    public int startTime;
    public int duration;
    public boolean isMust;

    public Command(DEV_ID _devID, int _duration, boolean _isMust)
    {
        this.devID = _devID;
        this.duration = _duration;
        this.isMust = _isMust;
    }

    public int getCmdEndTime()
    {
        return this.startTime + this.duration;
    }

    public boolean isCmdOverlapsWithWatchTime(int queryTime)
    {
        boolean insideBound = false;

        if(this.startTime <= queryTime && queryTime < getCmdEndTime())
        { // NOTE: the start time is inclusive, whereas the end time is exclusive. e.g.   [3,7)
            insideBound = true;
        }

        return insideBound;
    }

    public int compareTimeline(int queryTime)
    {
        if(this.getCmdEndTime() <= queryTime)
            return -1; //  Cmd ends before query

        if(this.startTime <= queryTime && queryTime < getCmdEndTime())
            return 0; // cmd overlaps

        return 1; // cmd starts after query
    }

    @Override
    public String toString()
    {
        String str = "";

        str += "[ " + this.devID.name() + ":" + startTime + ", " + (startTime + duration) + "]";

        return str;
    }
}
