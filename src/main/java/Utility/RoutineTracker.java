package Utility;

import CentralController.Controller;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/10/2019
 * @time 2:06 PM
 */
public class RoutineTracker
{
    Set<Integer> preRoutineSet;
    Set<Integer> postRoutineSet;
    public boolean isRoutineScheduled;

    public Routine routine;
    private List<String> devNameList;

    public Map<String, DEV_LOCK> devNameLockStatusMap;

    public int successfullyExecutedCmdIdx;
    public int currentlyExecutingCmdIdx;

    public DEV_STATUS todoFixThisApproach_getLastUsedDesiredStatus(String devName)
    { //TODO: insert the device-desired-status in lockTable
        DEV_STATUS devStatus = DEV_STATUS.NOT_INITIALIZED;

        int index;
        for(index = routine.commandList.size() - 1 ; 0 <= index ; --index)
        {
            if(devName.equals(routine.commandList.get(index).devName))
                break;
        }

        if(0 <= index)
            devStatus = routine.commandList.get(index).desiredStatus;

        return devStatus;
    }


    public int getRoutineID()
    {
        return this.routine.uniqueRoutineID;
    }

    public List<String> getDevNameListUsedInThisRtn()
    {
        return this.devNameList;
    }

    public RoutineTracker(Routine _routine)
    {
        this.preRoutineSet = new HashSet<>();
        this.postRoutineSet = new HashSet<>();
        this.isRoutineScheduled = false;

        this.routine = _routine;

        this.devNameLockStatusMap = new HashMap<>();
        this.successfullyExecutedCmdIdx = -1;
        this.currentlyExecutingCmdIdx = -1;
        this.devNameList = new ArrayList<>();

        for(Command cmd: this.routine.commandList)
        {
            String devName = cmd.devName;

            if(devName.equals(Controller.DEV_NAME_DUMMY))
                continue;

            this.devNameList.add(devName);
            this.devNameLockStatusMap.put(devName, DEV_LOCK.NOT_ACQUIRED); // create a list of devices touched by the routine
        }
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

    public DEV_LOCK getDevLockStatus(String devName)
    {
        assert(devName != Controller.DEV_NAME_DUMMY);

        if(!devNameLockStatusMap.containsKey(devName))
            return DEV_LOCK.NEVER_ACCESSED;

        return this.devNameLockStatusMap.get(devName);
    }



    private synchronized void recordCmdExcCompletion(int _successfullyExecutedCmdIdx)
    {
        if(-1 == _successfullyExecutedCmdIdx)
            return; // nothing to record yet.

        assert( (this.successfullyExecutedCmdIdx + 1) == _successfullyExecutedCmdIdx);

        this.successfullyExecutedCmdIdx = _successfullyExecutedCmdIdx;

        String succsExecutedDevName = this.routine.commandList.get(this.successfullyExecutedCmdIdx).devName;

        if(!succsExecutedDevName.equals(Controller.DEV_NAME_DUMMY))
        {
            boolean isUsedInFuture = false;
            //String currentDeviceName = this.devAccessSequence.get(this.successfullyExecutedCmdIdx);

            for(int futureIdx = (this.successfullyExecutedCmdIdx + 1) ; futureIdx < this.routine.commandList.size() ; ++futureIdx)
            {
                String futureDevices = this.routine.commandList.get(futureIdx).devName;

                if(futureDevices.equals(succsExecutedDevName))
                {
                    isUsedInFuture = true;
                    break;
                }
            }

            if(!isUsedInFuture)
                this.devNameLockStatusMap.put(succsExecutedDevName, DEV_LOCK.RELEASED);
        }
    }

    public synchronized int getNextCmdToExecute(int successfullyExecutedCmdIdx)
    {
        assert(this.isRoutineScheduled);

        this.recordCmdExcCompletion(successfullyExecutedCmdIdx);

        this.currentlyExecutingCmdIdx = 1 + this.successfullyExecutedCmdIdx;

        assert(this.currentlyExecutingCmdIdx < this.routine.commandList.size());

        String currentlyExecutingDevName = this.routine.commandList.get(this.currentlyExecutingCmdIdx).devName;

        if(!currentlyExecutingDevName.equals(Controller.DEV_NAME_DUMMY))
            this.devNameLockStatusMap.put(currentlyExecutingDevName, DEV_LOCK.EXECUTING);

        return this.currentlyExecutingCmdIdx;
    }
}
