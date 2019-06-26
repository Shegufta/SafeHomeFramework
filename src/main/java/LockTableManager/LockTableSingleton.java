package LockTableManager;

import ConcurrencyController.ConcurrencyControllerSingleton;
import RegistrationPkg.LockTableBluePrint;
import RegistrationPkg.RegistrationFactory;
import RegistrationPkg.RegistrationManager;
import RegistrationPkg.RegistrationType;
import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/3/2019
 * @time 10:34 AM
 */
public class LockTableSingleton
{
    private static String TAGstart;
    private static String TAGclassName;

    private static LockTableSingleton singleton;
    private boolean isInitialized;

    public static Object lockTableLockObject;
    private Map<DEV_ID, PerDevLockListManager> globalLockTable;

    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean isDisposed;
    private static long startTimeNanoSec;

    private int routineIDgenerator;

    private LockTableSingleton()
    {
        this.isDisposed = false;
        this.isInitialized = false;
        this.routineIDgenerator = 0;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
        System.nanoTime(); // dummy call... need it to increase the accuracy
        System.nanoTime(); // dummy call... need it to increase the accuracy
        LockTableSingleton.startTimeNanoSec = System.nanoTime();
    }
    ////////////////////////////////////////////////////////////////////
    public static synchronized LockTableSingleton getInstance()
    {
        if(null == LockTableSingleton.singleton)
        {
            LockTableSingleton.singleton = new LockTableSingleton();
        }

        return LockTableSingleton.singleton;
    }
    ////////////////////////////////////////////////////////////////////
    private int getNextRoutineID()
    {
        int nextRoutineID = this.routineIDgenerator;
        routineIDgenerator++;
        return nextRoutineID;
    }
    ////////////////////////////////////////////////////////////////////
    public void initDeviceList(final Set<DEV_ID> _devIdSet)
    {
        this.globalLockTable = new ConcurrentHashMap<>();

        LockTableSingleton.lockTableLockObject = new Object();

        LockTableSingleton.TAGstart = "@@@";
        LockTableSingleton.TAGclassName = this.getClass().getSimpleName();

        for(DEV_ID devID : _devIdSet)
        {
            globalLockTable.put(devID, new PerDevLockListManager(devID, DEV_STATUS.OFF));
            // TODO: currently all devices are initialized with "OFF"... fix this constraint
        }

        this.isInitialized = true;
    }
    ////////////////////////////////////////////////////////////////////
    public void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        assert(isInitialized);

        newRoutine.assignRoutineID(this.getNextRoutineID());

        assert(!getAllRoutineMap().containsKey(newRoutine.routineID));


        RegistrationType registrationType = RegistrationType.REG_APPEND_LAST;
        RegistrationManager registrationManager = RegistrationFactory.createRegistrationManager(registrationType);

        synchronized (LockTableSingleton.lockTableLockObject )
        {
            LockTableMetadata lockTableMetadata = this.getLockTableMetaData();
            LockTableBluePrint lockTableBluePrint = registrationManager.getLockTableBluePrint(lockTableMetadata, newRoutine);
            this.applyLockTableBluePrint(lockTableBluePrint, newRoutine);
            newRoutine.startExecution(); // NOTE: dont forget start routine execution! do it inside the lock
        }

        this.scheduleCheckForAvailableLockAndNotify();
    }
    ////////////////////////////////////////////////////////////////////
    public void commitRoutine(int committedRoutineID)
    {
        assert(isInitialized);

        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        this.printGlobalLockTable();

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                entry.getValue().commitRoutine(committedRoutineID);
            }
        }

        System.out.println("**************AFTER COMMIT************");
        this.printGlobalLockTable();

        this.scheduleCheckForAvailableLockAndNotify();
    }
    ////////////////////////////////////////////////////////////////////
    public void commandFinishes(int routineID, Command finishedCmd)
    {

        System.out.println("\t|||||| Command Finish : [Routine ID : " +  routineID + " | " + finishedCmd );
        this.printGlobalLockTable();
        this.scheduleCheckForAvailableLockAndNotify();
    }
    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    private void scheduleCheckForAvailableLockAndNotify()
    {
        this.ScheduleExecutor(0); // schedule immediately
    }
    ////////////////////////////////////////////////////////////////////
    private synchronized void ScheduleExecutor(int scheduleIntervalInMilliSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.allocateAvailableLocksAndNotify();},
                scheduleIntervalInMilliSec,
                TimeUnit.MILLISECONDS
        ); // reschedule
    }
    ////////////////////////////////////////////////////////////////////
    private void allocateAvailableLocksAndNotify()
    {
        assert(isInitialized);

        synchronized(LockTableSingleton.lockTableLockObject)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                entry.getValue().checkForAvailableLockAndNotify();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    private LockTableMetadata getLockTableMetaData()
    {
        assert(isInitialized);
        LockTableMetadata lockTableMetadata = new LockTableMetadata();

        //Map<DEV_ID, List<DevBasedRoutineMetadata>> metadataMap = new ConcurrentHashMap<>();

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                DEV_ID dev_id = entry.getKey();

                lockTableMetadata.AddRow( dev_id, entry.getValue().getMetadataList() );

                //metadataMap.put (dev_id, new ArrayList<>());
            }
        }


        return lockTableMetadata;
    }
    ////////////////////////////////////////////////////////////////////
    private void applyLockTableBluePrint(LockTableBluePrint _lockTableBluePrint, SelfExecutingRoutine _newRoutine)
    {
        assert(isInitialized);
        assert(_lockTableBluePrint.getAllDevSet().size() == this.globalLockTable.keySet().size());

        Map<Integer, SelfExecutingRoutine> allRoutineMap = this.getAllRoutineMap();

        if(_newRoutine == null)
        {
            for(DEV_ID devId : _lockTableBluePrint.getAllDevSet())
            {
                int bluePrintRowSize = _lockTableBluePrint.getRoutineIdList(devId).size();
                int globalTableRowSize = this.globalLockTable.get(devId).accessedRoutineList.size();

                assert(bluePrintRowSize == globalTableRowSize);
            }
        }
        else
        {
            assert(!allRoutineMap.containsKey(_newRoutine.routineID));
            allRoutineMap.put(_newRoutine.routineID, _newRoutine);
        }

        for(DEV_ID devId : _lockTableBluePrint.getAllDevSet())
        {
            List<Integer> bluePrintRoutineIDlist = _lockTableBluePrint.getRoutineIdList(devId);

            int globalLockTableRowSize = this.globalLockTable.get(devId).accessedRoutineList.size();

            boolean lockTypeAcquiredFound = false;
            int acquiredLockStartingIndex = 0;

            List<SelfExecutingRoutine> removeList = new ArrayList<>();

            for(int index = 0 ; index < globalLockTableRowSize ; index++)
            {
                SelfExecutingRoutine routineInGlobalLockTable = this.globalLockTable.get(devId).accessedRoutineList.get(index);
                DEV_LOCK globalLkTableLockStatus = routineInGlobalLockTable.getLockStatus(devId);
                int routineIDinLockTable = routineInGlobalLockTable.routineID;

                int routineIDInBlueprint = bluePrintRoutineIDlist.get(index);

                if(globalLkTableLockStatus == DEV_LOCK.RELEASED || globalLkTableLockStatus == DEV_LOCK.EXECUTING)
                {
                    if(lockTypeAcquiredFound)
                    {
                        System.out.println("ERROR: once even a single " + DEV_LOCK.ACQUIRED + "is seen, we should not see any other Released or Executing lock");
                        assert(false);
                    }
                    if(routineIDinLockTable != routineIDInBlueprint)
                    {
                        System.out.println("ERROR: blueprint should not change the routine position if lock_status is either RELEASED or EXECUTING");
                        assert (false);
                    }
                    acquiredLockStartingIndex++;
                }
                else
                {
                    lockTypeAcquiredFound = true;
                    removeList.add(routineInGlobalLockTable);
                }
            }

            for(SelfExecutingRoutine routineToRemove : removeList)
            {
                this.globalLockTable.get(devId).accessedRoutineList.remove(routineToRemove);
            }

            for( ; acquiredLockStartingIndex < bluePrintRoutineIDlist.size()  ; acquiredLockStartingIndex++)
            {
                int routineIDInBlueprint = bluePrintRoutineIDlist.get(acquiredLockStartingIndex);
                SelfExecutingRoutine routineFromBluePrint = allRoutineMap.get(routineIDInBlueprint);
                this.globalLockTable.get(devId).accessedRoutineList.add(routineFromBluePrint);
            }
        }
        this.rebuildAllPreAndPostSets();
    }
    ////////////////////////////////////////////////////////////////////
    private Map<Integer, SelfExecutingRoutine> getAllRoutineMap()
    {
        Map<Integer, SelfExecutingRoutine> allRoutineMap = new HashMap<>();

        for(PerDevLockListManager perDevLockListManager : this.globalLockTable.values())
        {
            for(SelfExecutingRoutine routine : perDevLockListManager.accessedRoutineList)
            {
                if(!allRoutineMap.containsKey(routine.routineID))
                    allRoutineMap.put(routine.routineID, routine);
            }
        }

        return allRoutineMap;
    }
    ////////////////////////////////////////////////////////////////////
    private void rebuildAllPreAndPostSets()
    {
        this.clearAllPreAndPostSets();

        Map<Integer, SelfExecutingRoutine> allRoutineMap = this.getAllRoutineMap();

        for(SelfExecutingRoutine routine : allRoutineMap.values())
        {
            Set<DEV_ID> devSetUsedInRtn = routine.getAllTouchedDevID();

            for(DEV_ID dev_id : devSetUsedInRtn)
            {
                assert(this.globalLockTable.get(dev_id).accessedRoutineList.contains(routine));

                int routineIndex = this.globalLockTable.get(dev_id).accessedRoutineList.indexOf(routine);

                for(int preIndex = 0 ; preIndex < routineIndex ; preIndex++)
                {
                    int preRoutineID = this.globalLockTable.get(dev_id).accessedRoutineList.indexOf(preIndex);
                    routine.preRoutineSet.add(preRoutineID);
                }

                int listSize = this.globalLockTable.get(dev_id).accessedRoutineList.size();
                for(int postIndex = routineIndex + 1 ; postIndex < listSize ; ++postIndex)
                {
                    int postRoutineID = this.globalLockTable.get(dev_id).accessedRoutineList.indexOf(postIndex);
                    routine.postRoutineSet.add(postRoutineID);
                }
            }

            Set<Integer> intersection = new HashSet<>(routine.preRoutineSet);
            intersection.retainAll(routine.postRoutineSet);

            assert(intersection.isEmpty()); // empty means no loop
        }
    }
    ////////////////////////////////////////////////////////////////////
    private void clearAllPreAndPostSets()
    {
        Map<Integer, SelfExecutingRoutine> allRoutineMap = this.getAllRoutineMap();

        for(SelfExecutingRoutine routine : allRoutineMap.values())
        {
            routine.clearPrePostRoutineSet();
        }

    }
    ////////////////////////////////////////////////////////////////////
    private String TAGaddThrdTime(final String TAG)
    {
        final int elapsedTimeMS = (int)(LockTableSingleton.getInstance().getElapsedTimeNanoSec()/1000000);
        final String threadName = Thread.currentThread().getName();

        return TAG +" | ThrdName = " + threadName + " | MS = " + elapsedTimeMS + " | ";
    }
    ////////////////////////////////////////////////////////////////////
    public static synchronized int getElapsedTimeNanoSec()
    {
        return (int)(System.nanoTime() - LockTableSingleton.startTimeNanoSec);
    }
    ////////////////////////////////////////////////////////////////////
    @Override
    public String toString()
    {
        String str = "----------------------------------------\n";
        synchronized (LockTableSingleton.lockTableLockObject)
        {
            for(Map.Entry<DEV_ID, PerDevLockListManager> entry : this.globalLockTable.entrySet())
            {
                str += "{" + entry.getKey() + "}" +  entry.getValue().toString() + "\n";
            }
        }

        str += "----------------------------------------\n";

        return str;
    }
    ////////////////////////////////////////////////////////////////////
    public void printGlobalLockTable()
    {
        System.out.println(this);
    }
    ////////////////////////////////////////////////////////////////////



//    public void registerRoutine(SelfExecutingRoutine newRoutine)
//    {
//        RegistrationType registrationType = RegistrationType.REG_APPEND_LAST;
//        RegistrationManager registrationManager = RegistrationFactory.createRegistrationManager(registrationType);
//
//        synchronized (LockTableSingleton.lockTableLockObject )
//        {
//            LockTableMetadata lockTableMetadata = this.getLockTableMetaData();
//            LockTableBluePrint lockTableBluePrint = registrationManager.getLockTableBluePrint(lockTableMetadata, newRoutine);
//            this.applyLockTableBluePrint(lockTableBluePrint, newRoutine);
//        }
//
//        this.allocateAvailableLocksAndNotify();

        /*
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

            for(DEV_ID devID : devNameSet)
            {
                this.globalLockTable.get(devID).registerRoutine(newRoutine, canScheduleBeforeAllAcquiredLock);
            }
            newRoutine.startExecution(); // NOTE: dont forget to add this step!
        }
        */
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
    //}

}
