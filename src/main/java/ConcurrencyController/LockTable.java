package ConcurrencyController;

import CentralController.Controller;
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





    /////////////////////////////////////////////////////////////////////////////////////////

    List<RoutineTracker> routineTrackerList;
    Map<String, PerDevLockTracker> devLockTable;

    public LockTable()
    {
        this.devLockTable = new HashMap<>();
        this.routineTrackerList = new ArrayList<>();
    }

    public void initLockTable(List<Device> devList)
    {
        for(Device dev:devList)
        {
            assert(!this.devLockTable.containsKey(dev.deviceName));

            if(dev.deviceName.equals(Controller.DEV_NAME_DUMMY))
                continue;

            this.devLockTable.put(dev.deviceName, new PerDevLockTracker(dev.deviceName,  DEV_STATUS.OFF)); // set the initial status to OFF // TODO: change the initial status
        }
    }

    private synchronized boolean isRoutineTrackerListContainsRoutineID(int routineID)
    {
        for(RoutineTracker rtnTracker : this.routineTrackerList)
        {
            if(rtnTracker.getRoutineID() == routineID);
            {
                return true;
            }
        }
        return false;
    }



    public synchronized void registerRoutine(Routine _routine)
    {
        assert(!this.devLockTable.isEmpty());

        int routineID = _routine.uniqueRoutineID;

        assert(!isRoutineTrackerListContainsRoutineID(routineID)); // check for fresh ID

        this.routineTrackerList.add(new RoutineTracker(_routine));
    }

    private RoutineTracker helper_getRoutineTracker(int routineID)
    {
        RoutineTracker routineTracker = null;

        for(RoutineTracker rt : this.routineTrackerList)
        {
            if(routineTracker.getRoutineID() == routineID)
            {
                routineTracker = rt;
                break;
            }
        }

        assert(routineTracker != null); // dont try to get a stale routine... this assumption should be ok for the primary version...

        return routineTracker;
    }

    private RoutineTracker helper_removeRoutineTracker(int routineID)
    {
        RoutineTracker routineTracker = this.helper_getRoutineTracker(routineID);
        this.routineTrackerList.remove(routineTracker);

        return routineTracker;
    }

    public synchronized List<RoutineTracker> helper_getUnschduledRoutineList()
    {
        List<Integer> unScheduledRoutineID = new ArrayList<>();
        List<RoutineTracker> unscheduledRtnTrkrLst = new ArrayList<>();

        for(RoutineTracker routineTracker : this.routineTrackerList)
        {
            if(!routineTracker.isRoutineScheduled)
                unScheduledRoutineID.add(routineTracker.getRoutineID());
        }

        for(int rtnID : unScheduledRoutineID)
        {
            unscheduledRtnTrkrLst.add(helper_getRoutineTracker(rtnID));
        }

        return unscheduledRtnTrkrLst;
    }


    public NextStep getNextStep(int routineID, int successExecutedCmdIdx)
    {
        assert(isRoutineTrackerListContainsRoutineID(routineID)); // ensure that routine already exists...

        int nextCmdIdx = this.routineTrackerList.get(routineID).getNextCmdToExecute(successExecutedCmdIdx);

        NextStep nextStep = new NextStep(routineID, nextCmdIdx, 0);

        return nextStep;
    }


    public synchronized void unregisterRoutine(int routineID)
    {
        synchronized (this.devLockTable)
        {
            RoutineTracker routineTracker = this.helper_removeRoutineTracker(routineID);
            List<String> devNameList = routineTracker.getDevNameListUsedInThisRtn();

            for (String devName : devNameList)
            {
                this.devLockTable.get(devName).commitRoutine(routineID);
            }
        }

    }


    public synchronized List<RoutineTracker> getPrepareToExecuteRoutineIDlist()
    {
        synchronized (this.devLockTable)
        {
            List<RoutineTracker> rtnTrkrlist_PreparedToSchedule = new ArrayList<>();

            List<RoutineTracker> unScheduledRoutineList = this.helper_getUnschduledRoutineList();
            if (unScheduledRoutineList.isEmpty())
                return rtnTrkrlist_PreparedToSchedule; // return empty list. nothing to schedule

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

            return rtnTrkrlist_PreparedToSchedule;
        }//synchronized (this.devLockTable)
    }

}
