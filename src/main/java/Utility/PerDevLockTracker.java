package Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/2/2019
 * @time 11:55 PM
 */
public class PerDevLockTracker
{
    public String devName;
    public DEV_STATUS currentStatus;
    public List<Integer> rtnWaitingList;
    public int currentlyExecutingRtnID;

    public PerDevLockTracker(String _devName, DEV_STATUS _devStatus)
    {
        this.rtnWaitingList = new ArrayList<>();
        this.devName = _devName;
        this.setDeviceStatus(_devStatus);
        this.currentlyExecutingRtnID = -1;
    }

    public void setDeviceStatus(DEV_STATUS _devStatus)
    {
        this.currentStatus = _devStatus;
    }

    public boolean isDevLocked()
    {
        return (this.currentlyExecutingRtnID != -1);
    }

    public void registerRoutineID(int _routineID, boolean isAcquiredAllLock)
    {
        if(isAcquiredAllLock)
            this.currentlyExecutingRtnID = _routineID;
        else
            this.rtnWaitingList.add(_routineID);
    }
}
