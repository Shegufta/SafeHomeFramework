package Temp;

import Utility.DEV_ID;

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

    public Command(DEV_ID _devID, int _duration)
    {
        this.devID = _devID;
        this.duration = _duration;
    }
}
