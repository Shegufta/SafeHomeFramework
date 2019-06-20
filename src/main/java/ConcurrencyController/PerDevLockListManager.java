package ConcurrencyController;

import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.*;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/19/2019
 * @time 3:49 PM
 */
public class PerDevLockListManager
{
    public DEV_ID devID;
    public DEV_STATUS committedStatus;
    public List<SelfExecutingRoutine> accessedRoutineList;

    public PerDevLockListManager(DEV_ID _devID, DEV_STATUS _initialStatus)
    {
        this.devID = _devID;
        this.committedStatus = _initialStatus;
        this.accessedRoutineList = Collections.synchronizedList(new ArrayList());
    }

    public synchronized int getRoutineIndex(int routineID)
    {
        int routineIndex = -1;

        for(SelfExecutingRoutine routine : this.accessedRoutineList)
        {
            routineIndex++;

            if(routine.routineID == routineID)
                return routineIndex;
        }

        return routineIndex;
    }

    public synchronized void commitRoutine(int committedRoutineID)
    {
        int committedRoutineIndex = getRoutineIndex(committedRoutineID);

        if(committedRoutineIndex != -1)
        {
            this.committedStatus = this.accessedRoutineList.get(committedRoutineIndex).getLastSuccessfullySetDevStatus(this.devID);
            this.removeAccessedRoutineSubList(0, committedRoutineIndex);
        }
    }

    private synchronized void removeAccessedRoutineSubList(int startInclusiveIndex, int endInclusiveIndex)
    {
        assert(startInclusiveIndex <= endInclusiveIndex);

        for(int I = startInclusiveIndex ; I <= endInclusiveIndex ; I++)
        {
            this.accessedRoutineList.get(I).Dispose();
        }

        this.accessedRoutineList.subList(startInclusiveIndex, (endInclusiveIndex +1)).clear();
    }

    public synchronized void checkForAvailableLockAndNotify()
    {
        System.out.println("\t\t\t #### inside notify function");
        for(SelfExecutingRoutine routine : this.accessedRoutineList)
        {
            if(routine.getLockStatus(this.devID) == DEV_LOCK.RELEASED)
            {
                System.out.println("\t\t\t\t #### lock for " + this.devID.name() + " is => " + DEV_LOCK.RELEASED.name());
                continue;
            }

            if(routine.getLockStatus(this.devID) == DEV_LOCK.EXECUTING)
            {
                System.out.println("\t\t\t\t #### lock for " + this.devID.name() + " is => " + DEV_LOCK.EXECUTING.name());
                break;
            }
            else if(routine.getLockStatus(this.devID) == DEV_LOCK.ACQUIRED)
            {
                System.out.println("\t\t\t\t #### lock for " + this.devID.name() + " is => " + DEV_LOCK.ACQUIRED.name());
                System.out.println("\t\t\t\t\t #### set lock to Executing and Notify device" + this.devID.name());
                routine.setLockStatus(this.devID, DEV_LOCK.EXECUTING);
                routine.notifyToCheckLockInRelevantCommandChain(this.devID);
                break;
            }
        }
    }

    public synchronized boolean helper_isDuplicateRoutine(SelfExecutingRoutine newRoutine)
    {
        for(SelfExecutingRoutine existingRtn : accessedRoutineList)
        {
            if(existingRtn.routineID == newRoutine.routineID)
                return true;
        }

        return false;
    }

    public synchronized void registerRoutine(SelfExecutingRoutine newRoutine, boolean canScheduleBeforeAllAcquiredLock)
    {
        assert(!this.helper_isDuplicateRoutine(newRoutine));

        if(canScheduleBeforeAllAcquiredLock)
        {
            int insertIndex = 0;
            for(SelfExecutingRoutine existingRtn : this.accessedRoutineList)
            {
                DEV_LOCK existingLock = existingRtn.getLockStatus(this.devID);
                if(existingLock == DEV_LOCK.RELEASED || existingLock == DEV_LOCK.EXECUTING)
                {
                    existingRtn.postRoutineSet.add(newRoutine.routineID);
                    newRoutine.preRoutineSet.add(existingRtn.routineID);
                    insertIndex++;
                }
                else
                {
                    existingRtn.preRoutineSet.add(newRoutine.routineID);
                    newRoutine.postRoutineSet.add(existingRtn.routineID);
                }
            }

            this.accessedRoutineList.add(insertIndex, newRoutine);
        }
        else
        {
            for(SelfExecutingRoutine existingRtn : this.accessedRoutineList)
            {
                existingRtn.postRoutineSet.add(newRoutine.routineID);
                newRoutine.preRoutineSet.add(existingRtn.routineID);
            }

            this.accessedRoutineList.add(newRoutine);
        }


    }

    public synchronized Set<Integer> getPreSet()
    {
        Set<Integer> preSet = new HashSet<>();

        for(SelfExecutingRoutine routine : this.accessedRoutineList)
        {
            DEV_LOCK lockStatus = routine.getLockStatus(this.devID);
            if(lockStatus == DEV_LOCK.RELEASED || lockStatus == DEV_LOCK.EXECUTING)
            {
                preSet.add(routine.routineID);
                preSet.addAll(routine.preRoutineSet);
            }
        }

        return preSet;
    }

    public synchronized Set<Integer> getPostSet()
    {
        Set<Integer> postSet = new HashSet<>();

        for(SelfExecutingRoutine routine : this.accessedRoutineList)
        {
            DEV_LOCK lockStatus = routine.getLockStatus(this.devID);
            if(lockStatus == DEV_LOCK.ACQUIRED)
            {
                postSet.add(routine.routineID);
                postSet.addAll(routine.postRoutineSet);
            }
        }

        return postSet;
    }
}
