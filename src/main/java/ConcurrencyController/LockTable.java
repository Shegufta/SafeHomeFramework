package ConcurrencyController;

import CentralController.Controller;
import Utility.*;

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
        public Map<String, DEV_LOCK> devNameLockStatusMap;
        public List<String> devAccessSequence;
        public int successfullyExecutedCmdIdx;
        public int currentlyExecutingCmdIdx;

        public RoutineIDDevListRoutineStatus(Routine routine)
        {
            this.routineID = routine.uniqueRoutineID;
            this.isRoutineScheduled = false;
            this.devNameLockStatusMap = new HashMap<>();
            this.devAccessSequence = new ArrayList<>();
            this.successfullyExecutedCmdIdx = -1;
            this.currentlyExecutingCmdIdx = -1;

            for(Command cmd: routine.commandList)
            {
                String devName = cmd.devName;
                this.devAccessSequence.add(devName); // store the command sequence

                if(devName.equals(Controller.DEV_NAME_DUMMY))
                    continue;

                this.devNameLockStatusMap.put(devName, DEV_LOCK.NOT_ACQUIRED); // create a list of devices touched by the routine
            }
        }

        public DEV_LOCK devLockStatus(String devName)
        {
            assert(devName != Controller.DEV_NAME_DUMMY);

            if(!devNameLockStatusMap.containsKey(devName))
                return DEV_LOCK.NEVER_ACCESSED;

            return this.devNameLockStatusMap.get(devName);
        }

        public void scheduleRoutine()
        {
            this.isRoutineScheduled = true;
            this.successfullyExecutedCmdIdx = -1;

            for(String devName: this.devNameLockStatusMap.keySet())
            {
                this.devNameLockStatusMap.put(devName, DEV_LOCK.ACQUIRED);
            }
        }

        private void recordCmdExcCompletion(int _successfullyExecutedCmdIdx)
        {
            if(-1 == _successfullyExecutedCmdIdx)
                return; // nothing to record yet.

            assert( (this.successfullyExecutedCmdIdx + 1) == _successfullyExecutedCmdIdx);

            this.successfullyExecutedCmdIdx = _successfullyExecutedCmdIdx;

            String succsExecutedDevName = this.devAccessSequence.get(this.successfullyExecutedCmdIdx);

            if(!succsExecutedDevName.equals(Controller.DEV_NAME_DUMMY))
            {
                boolean isUsedInFuture = false;
                String currentDeviceName = this.devAccessSequence.get(this.successfullyExecutedCmdIdx);

                for(int idx = (this.successfullyExecutedCmdIdx + 1) ; idx < this.devAccessSequence.size() ; ++idx)
                {
                    String futureDevices = this.devAccessSequence.get(idx);

                    if(futureDevices.equals(currentDeviceName))
                    {
                        isUsedInFuture = true;
                        break;
                    }
                }

                if(!isUsedInFuture)
                    this.devNameLockStatusMap.put(succsExecutedDevName, DEV_LOCK.RELEASED);
            }
        }

        public int getNextCmdToExecute(int successfullyExecutedCmdIdx)
        {
            assert(this.isRoutineScheduled);

            this.recordCmdExcCompletion(successfullyExecutedCmdIdx);

            this.currentlyExecutingCmdIdx = 1 + this.successfullyExecutedCmdIdx;

            assert(this.currentlyExecutingCmdIdx < devAccessSequence.size());

            String currentlyExecutingDevName = this.devAccessSequence.get(this.currentlyExecutingCmdIdx);

            if(!currentlyExecutingDevName.equals(Controller.DEV_NAME_DUMMY))
                this.devNameLockStatusMap.put(currentlyExecutingDevName, DEV_LOCK.EXECUTING);

            return this.currentlyExecutingCmdIdx;
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

            if(dev.deviceName.equals(Controller.DEV_NAME_DUMMY))
                continue;

            this.devName_LockedRtnIDMap.put(dev.deviceName, UNLOCKED);
        }
    }

    public void unregisterRoutine(int routineID)
    {
        assert(!devName_LockedRtnIDMap.isEmpty());
        assert(this.routineID_DeviceListMAP.containsKey(routineID));

        for(String devName : this.routineID_DeviceListMAP.get(routineID).devNameLockStatusMap.keySet())
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


    public NextStep getNextStep(int routineID, int successExecutedCmdIdx)
    {
        int nextCmdIdx = routineID_DeviceListMAP.get(routineID).getNextCmdToExecute(successExecutedCmdIdx);

        NextStep nextStep = new NextStep(routineID, nextCmdIdx, 0);

        return nextStep;
    }
    public synchronized void recordCmdExcCompletion(int routineID, int successfullyExecutedCmdIdx)
    {
        routineID_DeviceListMAP.get(routineID).successfullyExecutedCmdIdx = successfullyExecutedCmdIdx;
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

            for(String devName : routineIDDevListRoutineStatus.devNameLockStatusMap.keySet())
            {
                if(devName.equals(Controller.DEV_NAME_DUMMY))
                    continue;

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

                for(String devName : routineIDDevListRoutineStatus.devNameLockStatusMap.keySet())
                {
                    if(devName.equals(Controller.DEV_NAME_DUMMY))
                        continue;

                    assert(availableDevList.contains(devName));
                    availableDevList.remove(devName);
                    this.devName_LockedRtnIDMap.put(devName, candidateRoutineID); // Update device Lock
                }
            }
        }

        return routineListToSchedule;
    }
}
