package ConcurrencyController;

import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/3/2019
 * @time 10:34 AM
 */
public class LockTable
{
    Map<DEV_ID, PerDevLockListManager> lockTable;

    public LockTable(final Set<DEV_ID> _devIdSet)
    {
        lockTable = new ConcurrentHashMap<>();

        for(DEV_ID devID : _devIdSet)
        {
            lockTable.put(devID, new PerDevLockListManager(devID, DEV_STATUS.OFF));
            // TODO: currently all devices are initialized with "OFF"... fix this constraint
        }
    }

    public synchronized void allocateAvailableLocksAndNotify()
    {
        synchronized (this.lockTable)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.lockTable.entrySet())
            {
                entry.getValue().checkForAvailableLockAndNotify();
            }
        }
    }

    public synchronized void commitRoutine(int committedRoutineID)
    {
        synchronized (this.lockTable)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.lockTable.entrySet())
            {
                entry.getValue().commitRoutine(committedRoutineID);
            }
        }
    }

    public synchronized void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        synchronized (this.lockTable)
        {
            boolean canScheduleBeforeAllAcquiredLock = true;

            Set<Integer> preRtnSet = new HashSet<>();
            Set<Integer> postRtnSet = new HashSet<>();
            final Set<DEV_ID> devNameSet = newRoutine.getAllTouchedDevID();

            for(DEV_ID devID : devNameSet)
            {
                preRtnSet.addAll( this.lockTable.get(devID).getPreSet()); //TODO: assuming that we will add the new routine before all 'A' locks. fix this assumption
                postRtnSet.addAll(this.lockTable.get(devID).getPostSet());//TODO: assuming that we will add the new routine before all 'A' locks. fix this assumption

                Set<Integer> intersection = new HashSet<>(preRtnSet);
                intersection.retainAll(postRtnSet);

                if (!intersection.isEmpty())
                {
                    canScheduleBeforeAllAcquiredLock = false;
                    break;
                }
            }

            for(DEV_ID devID : devNameSet)
            {
                this.lockTable.get(devID).registerRoutine(newRoutine, canScheduleBeforeAllAcquiredLock);
            }
            newRoutine.startExecution(); // NOTE: dont forget to add this step!
        }

        this.allocateAvailableLocksAndNotify();



/**
 * TODO: do not remove this code now... this is the old algorithm
        for (RoutineTracker unscheduledRtn : unScheduledRoutineList)
        {
            boolean canSchedule = true;

            Set<Integer> preRtnSet = new HashSet<>();
            Set<Integer> postRtnSet = new HashSet<>();

            List<String> devNameList = unscheduledRtn.getDevNameListUsedInThisRtn();

            for (String devName : devNameList)
            {
                List<PerDevLockTracker.RtnIDLckStatusTuple> perDevLckStatusLst = this.devLockTable.get(devName).getLockStatus();


                for (PerDevLockTracker.RtnIDLckStatusTuple perRtnLkStatus : perDevLckStatusLst)
                {
                    if (perRtnLkStatus.lockStatus == DEV_LOCK.RELEASED || perRtnLkStatus.lockStatus == DEV_LOCK.EXECUTING)
                    {
                        preRtnSet.add(perRtnLkStatus.routineID);
                        preRtnSet.addAll(perRtnLkStatus.preRoutineSet);
                    }
                    else if (perRtnLkStatus.lockStatus == DEV_LOCK.ACQUIRED)
                    {
                        postRtnSet.add(perRtnLkStatus.routineID);
                        postRtnSet.addAll(perRtnLkStatus.postRoutineSet);
                    }
                    else
                    {
                        System.out.println("ERROR: RtnID: " + perRtnLkStatus.routineID + ", lock status = " + perRtnLkStatus.lockStatus.name());
                        assert (false); // should not execute this line
                    }
                }

                Set<Integer> intersection = new HashSet<>(preRtnSet);
                intersection.retainAll(postRtnSet);

                if (!intersection.isEmpty())
                {
                    canSchedule = false;
                    break;
                }
            }

            if (canSchedule)
            {
                rtnTrkrlist_PreparedToSchedule.add(unscheduledRtn);

                unscheduledRtn.scheduleRoutine();
                for (String devName : devNameList)
                {
                    this.devLockTable.get(devName).registerRoutine(unscheduledRtn);
                }
            }


        }//for (RoutineTracker unscheduledRtn : unScheduledRoutineList)
        */
    }

}
