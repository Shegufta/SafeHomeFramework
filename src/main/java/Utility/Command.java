package Utility;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:31 AM
 */
public class Command
{
    public Command(String _devName, DEV_STATUS _desiredStatus, int _durationSecond)
    {
        this.devName = _devName;
        this.desiredStatus = _desiredStatus;
        this.durationSecond = _durationSecond; // 0 for one time commands e.g. turn on light. A value for long running routine e.g. turn on light for 30 min

    }

    public String devName;
    public DEV_STATUS desiredStatus;
    public int durationSecond; // 0 means short command.

    public boolean isLongCommand()
    {
        if(0 < this.durationSecond)
            return true;
        else
            return false;
    }

    public int getDurationInSecond()
    {
        return this.durationSecond;
    }
}
