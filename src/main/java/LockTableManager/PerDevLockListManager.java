package LockTableManager;

import ConcurrencyController.ConcurrencyControllerSingleton;
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

    private static String TAGstart;
    private static String TAGclassName;

    ///////////////////////////////////////////////////////////////////////////
    public PerDevLockListManager(DEV_ID _devID, DEV_STATUS _initialStatus)
    {
        this.devID = _devID;
        this.committedStatus = _initialStatus;
        this.accessedRoutineList = Collections.synchronizedList(new ArrayList());

        PerDevLockListManager.TAGstart = "+++";
        PerDevLockListManager.TAGclassName = this.getClass().getSimpleName();
    }
    ///////////////////////////////////////////////////////////////////////////
    public void checkForAvailableLockAndNotify()
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        boolean isNotified = false;
        SelfExecutingRoutine targetRoutine = null;

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for( SelfExecutingRoutine routine : this.accessedRoutineList)
            {
                if(routine.getLockStatus(this.devID) == DEV_LOCK.RELEASED)
                {
                    System.out.println("\t" + this.TAGaddThrdTime(TAG) + "Lock(" + this.devID + ") = " + DEV_LOCK.RELEASED);
                    continue;
                }

                if(routine.getLockStatus(this.devID) == DEV_LOCK.EXECUTING)
                {
                    System.out.println("\t" + this.TAGaddThrdTime(TAG) + "Lock(" + this.devID + ") = " + DEV_LOCK.EXECUTING);
                    break;
                }
                else if(routine.getLockStatus(this.devID) == DEV_LOCK.ACQUIRED )
                {
                    //System.out.println("\n\n\n+++++++++++++++++++++++++++++++++++++++++");
                    System.out.println("\t" + this.TAGaddThrdTime(TAG) + "Lock(" + this.devID + ") = " + DEV_LOCK.ACQUIRED);

                    boolean isInFront = routine.isWaitingForExecution(this.devID);

                    if(isInFront)
                    {
                        System.out.println("\t\t" + this.TAGaddThrdTime(TAG) + "set lock to Executing and Notify device" + this.devID.name());
                        routine.setLockStatus(this.devID, DEV_LOCK.EXECUTING);
                        targetRoutine = routine;
                        ////routine.notifyToCheckLockInRelevantCommandChain(this.devID); // call it outside the sync block
                    }

                    break;
                }
            }
        }//synchronized (LockTableSingleton.lockTableLockObject)

        if(targetRoutine != null)
            targetRoutine.notifyToCheckLockInRelevantCommandChain(this.devID); // call it outside the sync block

    }
    ///////////////////////////////////////////////////////////////////////////
    public void commitRoutine(int committedRoutineID)
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;
        System.out.println(this.TAGaddThrdTime(TAG) + "committedRoutineID = " + committedRoutineID);

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            int committedRoutineIndex = getRoutineIndex(committedRoutineID);

            if(committedRoutineIndex != -1)
            {
                this.committedStatus = this.accessedRoutineList.get(committedRoutineIndex).getLastSuccessfullySetDevStatus(this.devID);
                this.removeAccessedRoutineSubList(0, committedRoutineIndex);
            }
        }

    }
    ///////////////////////////////////////////////////////////////////////////
    private void removeAccessedRoutineSubList(int startInclusiveIndex, int endInclusiveIndex)
    {
        assert(startInclusiveIndex <= endInclusiveIndex);

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(int I = startInclusiveIndex ; I <= endInclusiveIndex ; I++)
            {
                this.accessedRoutineList.get(I).Dispose();
            }
        }

        this.accessedRoutineList.subList(startInclusiveIndex, (endInclusiveIndex +1)).clear();
    }
    ///////////////////////////////////////////////////////////////////////////
    private int getRoutineIndex(int routineID)
    {
        int routineIndex = -1;

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(SelfExecutingRoutine routine : this.accessedRoutineList)
            {
                routineIndex++;

                if(routine.routineID == routineID)
                    return routineIndex;
            }
        }

        return routineIndex;
    }
    ///////////////////////////////////////////////////////////////////////////
    public List<DevBasedRoutineMetadata> getMetadataList()
    {
        List<DevBasedRoutineMetadata> metadataList = Collections.synchronizedList(new ArrayList());

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(SelfExecutingRoutine routine : this.accessedRoutineList)
            {
                metadataList.add(new DevBasedRoutineMetadata(this.devID, routine));
            }
        }

        return metadataList;
    }
    ///////////////////////////////////////////////////////////////////////////
    private String TAGaddThrdTime(final String TAG)
    {
        final int elapsedTimeMS = (int)(LockTableSingleton.getInstance().getElapsedTimeNanoSec()/1000000);
        final String threadName = Thread.currentThread().getName();

        return TAG +" | ThrdName = " + threadName + " | MS = " + elapsedTimeMS + " | ";
    }
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString()
    {
        synchronized (this.accessedRoutineList)
        {
            String str = "";

            if(this.accessedRoutineList.isEmpty())
            {
                str = " EMPTY ";
            }
            else
            {
                for(SelfExecutingRoutine routine : this.accessedRoutineList)
                {
                    int routineID = routine.routineID;
                    DEV_LOCK lockStatus = routine.getLockStatus(this.devID);
                    str += " [RtnID:" + routineID + "|" + lockStatus + "] ";
                }
            }

            return str;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /*
    public synchronized boolean helper_isDuplicateRoutine(SelfExecutingRoutine newRoutine)
    {
        for(SelfExecutingRoutine existingRtn : accessedRoutineList)
        {
            if(existingRtn.routineID == newRoutine.routineID)
                return true;
        }

        return false;
    }
    */

    /*
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
    */

    /*
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
    */
    /*
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
    */
}
