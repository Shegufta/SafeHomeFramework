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
    public static final int UNLOCKED = -1;

    public class RoutineIDDevListRoutineStatus
    {
        public int routineID;
        public boolean isRoutineScheduled;
        public List<String> deviceList;

        public RoutineIDDevListRoutineStatus(Routine routine)
        {
            this.routineID = routine.uniqueRoutineID;
            this.isRoutineScheduled = false;
            this.deviceList = new ArrayList<>();

            for(Command cmd: routine.commandList)
            {
                String devName = cmd.devName;
                this.deviceList.add(devName); // create a list of devices touched by the routine
            }
        }
    }

    /////////////Map<String, PerDevLockTracker> lockTable;
    Map<String, Integer> devName_LockedRtnIDMap;

    Map<Integer, RoutineIDDevListRoutineStatus> routineID_DeviceListMAP;

    public LockTable()
    {
        /////////////this.lockTable = new HashMap<>();
        this.devName_LockedRtnIDMap = new HashMap<>();
        this.routineID_DeviceListMAP = new HashMap<>();
    }

    public void initLockTable(List<Device> devList)
    {
        for(Device dev:devList)
        {
            assert(!this.devName_LockedRtnIDMap.containsKey(dev.deviceName));
            this.devName_LockedRtnIDMap.put(dev.deviceName, UNLOCKED);
        }
    }

    public void unregisterRoutine(int routineID)
    {
        assert(!devName_LockedRtnIDMap.isEmpty());
        assert(this.routineID_DeviceListMAP.containsKey(routineID));

        for(String devName : this.routineID_DeviceListMAP.get(routineID).deviceList)
        {
            assert(this.devName_LockedRtnIDMap.containsKey(devName));

            if(this.devName_LockedRtnIDMap.get(devName) == routineID)
            {
                this.devName_LockedRtnIDMap.put(devName, UNLOCKED); // unlock allocated resources
            }
        }

        this.routineID_DeviceListMAP.remove(routineID);
    }

    public synchronized void registerRoutine(Routine _routine)
    {
        int routineID = _routine.uniqueRoutineID;

        assert(!devName_LockedRtnIDMap.isEmpty());
        assert(!this.routineID_DeviceListMAP.containsKey(routineID));

        //if(!this.routineID_DeviceListMAP.containsKey(routineID))
        this.routineID_DeviceListMAP.put(routineID, new RoutineIDDevListRoutineStatus(_routine));
    }

    public synchronized List<Integer> getPrepareToExecuteRoutineIDlist()
    {

        List<Integer> routineListToSchedule = new ArrayList<>();

        ///////////////////// Get the available device list

        List<String> availableDevList = new ArrayList<>();
        for(Map.Entry<String, Integer> tuple : this.devName_LockedRtnIDMap.entrySet())
        {
            if(tuple.getValue() == UNLOCKED)
            {
                availableDevList.add(tuple.getKey());
            }
        }

        for( RoutineIDDevListRoutineStatus routineIDDevListRoutineStatus : this.routineID_DeviceListMAP.values())
        {
            if(routineIDDevListRoutineStatus.isRoutineScheduled)
                continue;

            int candidateRoutineID = routineIDDevListRoutineStatus.routineID;
            boolean isAllLocksAvailable = true;

            for(String devName : routineIDDevListRoutineStatus.deviceList)
            {
                if(!availableDevList.contains(devName))
                {
                    isAllLocksAvailable = false;
                    break;
                }
            }

            if(isAllLocksAvailable)
            {
                routineListToSchedule.add(candidateRoutineID);
                routineID_DeviceListMAP.get(candidateRoutineID).isRoutineScheduled = true;

                for(String devName : routineIDDevListRoutineStatus.deviceList)
                {
                    assert(availableDevList.contains(devName));
                    availableDevList.remove(devName);
                    this.devName_LockedRtnIDMap.put(devName, candidateRoutineID); // Update device Lock
                }
            }
        }

        return routineListToSchedule;
    }
}
