package ConcurrencyController;

import Utility.Command;
import Utility.Device;
import Utility.PerDevLockTracker;
import Utility.Routine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/3/2019
 * @time 10:34 AM
 */
public class LockTable
{
    Map<String, PerDevLockTracker> lockTable;

    public LockTable()
    {
        this.lockTable = new HashMap<>();
    }

    public void initLockTable(List<Device> devList)
    {
        for(Device dev:devList)
        {
            assert(!this.lockTable.containsKey(dev.deviceName));

            this.lockTable.put(dev.deviceName, new PerDevLockTracker(dev.deviceName, dev.currentStatus));
        }
    }

    public void scheduleRoutine(Routine routine)
    {
        boolean acquiredAllLock = true;

        for(Command cmd: routine.commandList)
        {
            String devName = cmd.devName;
            assert( this.lockTable.containsKey(devName) );

            if(this.lockTable.get(devName).isDevLocked())
            {
                acquiredAllLock = false;
                break;
            }
        }

        int routineID = routine.uniqueRoutineID;

        for(Command cmd: routine.commandList)
        {
            String devName = cmd.devName;
            this.lockTable.get(devName).registerRoutineID(routineID, acquiredAllLock);
        }
    }

}
