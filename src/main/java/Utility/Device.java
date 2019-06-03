package Utility;

import java.sql.Time;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:24 AM
 */
public class Device
{
    public String deviceName;
    public DEV_STATUS currentStatus;

    public Device(String _devName, DEV_STATUS _currentStatus)
    {
        this.deviceName = _devName;
        this.currentStatus = _currentStatus;
    }
}
