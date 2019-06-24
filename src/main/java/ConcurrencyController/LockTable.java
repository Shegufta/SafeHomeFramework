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
    private static String TAGstart;
    private static String TAGclassName;

    @Override
    public String toString()
    {
        String str = "----------------------------------------\n";
        synchronized (this.globalLockTable)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                str += "{" + entry.getKey() + "}" +  entry.getValue().toString() + "\n";
            }
        }

        str += "----------------------------------------\n";

        return str;
    }

    Map<DEV_ID, PerDevLockListManager> globalLockTable;
    /*
    private synchronized Map<DEV_ID, List<DevBasedRoutineMetadata>> getLockTableMetaData()
    {
        Map<DEV_ID, List<DevBasedRoutineMetadata>> metadataMap = new HashMap<>();

        for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
        {
            DEV_ID dev_id = entry.getKey();

            metadataMap.put (dev_id, new ArrayList<>());

            str += "{" + entry.getKey() + "}" +  entry.getValue().toString() + "\n";
        }

    }
*/
    public LockTable(final Set<DEV_ID> _devIdSet)
    {
        this.globalLockTable = new ConcurrentHashMap<>();

        LockTable.TAGstart = "@@@";
        LockTable.TAGclassName = this.getClass().getSimpleName();

        for(DEV_ID devID : _devIdSet)
        {
            globalLockTable.put(devID, new PerDevLockListManager(devID, DEV_STATUS.OFF));
            // TODO: currently all devices are initialized with "OFF"... fix this constraint
        }
    }

    private String TAGaddThrdTime(final String TAG)
    {
        final int elapsedTimeMS = (int)(ConcurrencyControllerSingleton.getInstance().getElapsedTimeNanoSec()/1000000);
        final String threadName = Thread.currentThread().getName();

        return TAG +" | ThrdName = " + threadName + " | MS = " + elapsedTimeMS + " | ";
    }

    public void allocateAvailableLocksAndNotify()
    {
        synchronized (this.globalLockTable)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                entry.getValue().checkForAvailableLockAndNotify();
            }
        }
    }

    public void printGlobalLockTable()
    {
        System.out.println(this);
    }

    public void commitRoutine(int committedRoutineID)
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        System.out.println(this.TAGaddThrdTime(TAG) + " trying to enter synchronized(this.globalLockTable) | committedRoutineID = " + committedRoutineID);

        synchronized (this.globalLockTable)
        {
            System.out.println( "\t"+ this.TAGaddThrdTime(TAG) + " INSIDE sync block");
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                entry.getValue().commitRoutine(committedRoutineID);
            }
        }
    }

    public void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        synchronized (this.globalLockTable)
        {
            boolean canScheduleBeforeAllAcquiredLock = true;

            Set<Integer> preRtnSet = new HashSet<>();
            Set<Integer> postRtnSet = new HashSet<>();
            final Set<DEV_ID> devNameSet = newRoutine.getAllTouchedDevID();

            for(DEV_ID devID : devNameSet)
            {
                preRtnSet.addAll( this.globalLockTable.get(devID).getPreSet()); //TODO: assuming that we will add the new routine before all 'A' locks. fix this assumption
                postRtnSet.addAll(this.globalLockTable.get(devID).getPostSet());//TODO: assuming that we will add the new routine before all 'A' locks. fix this assumption

                Set<Integer> intersection = new HashSet<>(preRtnSet);
                intersection.retainAll(postRtnSet);

                if (!intersection.isEmpty())
                {
                    canScheduleBeforeAllAcquiredLock = false;
                    break;
                }
            }
    /*
            Current scenario
            Dev1= R R R R R A1 A2 A3 A4
            dev2= R R R R R A1 A2 A3 A4

            R(d1, d2, d3)...
                R - [t1`,t2]
            c1, cRn,

            * Policy 1: Append to the end
            * Policy 2: Append to the beginning -> if violates, use Policy 1
            (*) Policy 3: Append based on timeline
            Policy 4: Based on fewest devices first
            Policy 5: Short routine(at most 3) first
            Policy 6: Greedy approach : earliest job first
            Policy 7: Acquire all lock first ( not parallel)


            Interfaces:
                1. Ask: can I insert routine Ri at location Index_j
                2. Insert (Ri, index_j)
                3. canSwap(Ri,Rj)
                4. swap(Ri,Rj)
                5. lockTable.getAllRoutine(dev_n)
                6. getWaitQueue()
     */

            for(DEV_ID devID : devNameSet)
            {
                this.globalLockTable.get(devID).registerRoutine(newRoutine, canScheduleBeforeAllAcquiredLock);
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
