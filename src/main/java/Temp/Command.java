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

    @Override
    public String toString()
    {
        String str = "";

        str += "[ " + this.devID.name() + ":" + startTime + ", " + (startTime + duration) + "]";

        return str;
    }
}
