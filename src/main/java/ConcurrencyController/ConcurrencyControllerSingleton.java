package ConcurrencyController;

import Executor.ExecutorSingleton;
import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/5/2019
 * @time 1:28 AM
 */
public class ConcurrencyControllerSingleton
{
    private static ConcurrencyControllerSingleton singleton;

    private boolean isDisposed;
    private LockTable lockTable;

    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;

    private ConcurrencyControllerSingleton()
    {
        this.isDisposed = false;
        //this.lockTable = new LockTable();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
    }

    public synchronized void initDeviceList(final Set<DEV_ID> devIDSet)
    {
        this.lockTable = new LockTable(devIDSet);
    }/// SBA: new API

    public synchronized void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        this.lockTable.registerRoutine(newRoutine);
    }

    public synchronized void commitRoutine(int committedRoutineID)
    {
        this.lockTable.commitRoutine(committedRoutineID);
    }

    public synchronized void commandFinishes()
    {
        this.scheduleCheckForAvailableLockAndNotify();
    }

    public synchronized void scheduleCheckForAvailableLockAndNotify()
    {
        this.ScheduleExecutor(0); // schedule immediately
    }

    private synchronized void checkForAvailableLockAndNotify()
    {
        synchronized (this.lockTable.lockTable)
        {
            this.lockTable.allocateAvailableLocksAndNotify();
        }
    }

    //////////////////////////////////////////////////////////////////////////


    public void InitDeviceList(List<Device> devList)
    {
        this.lockTable.initLockTable(devList);
    }



    public void RegisterRoutine(Routine _routine)
    {
        this.lockTable.registerRoutine(_routine);
        this.ScheduleExecutor(0); // schedule immediately
    }

    /*
    public List<Integer> getPrepareToExecuteRoutineIDlist()
    {
        return this.lockTable.getPrepareToExecuteRoutineIDlist();
    }
    */

    public void FinishRoutineExecution(Integer _routineID)
    {
        this.UnregisterRoutine(_routineID);
    }

    private void UnregisterRoutine(Integer _routineID)
    {
        this.lockTable.unregisterRoutine(_routineID);
        this.ScheduleExecutor(0); // schedule immediately
    }


    public NextStep getNextStep(int routineID, int successExecutedCmdIdx)
    {
        return this.lockTable.getNextStep(routineID, successExecutedCmdIdx);
    }


    private void checkForAvailableRoutines()
    {
        System.out.println("Check for available routines....");
        List<RoutineTracker> routineToInitiateList = this.lockTable.getPrepareToExecuteRoutineIDlist();

        if(!routineToInitiateList.isEmpty())
        {
            ExecutorSingleton.getInstance().ExecuteRoutines(routineToInitiateList);
        }
    }

    private synchronized void ScheduleExecutor(int scheduleIntervalInMilliSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.checkForAvailableLockAndNotify();},
                scheduleIntervalInMilliSec,
                TimeUnit.MILLISECONDS
        ); // reschedule
//        this.scheduledFuture = this.scheduledExecutorService.schedule(
//                ()-> {this.checkForAvailableRoutines();},
//                scheduleIntervalInMilliSec,
//                TimeUnit.MILLISECONDS
//        ); // reschedule
    }

    public static synchronized ConcurrencyControllerSingleton getInstance()
    {
        if(null == ConcurrencyControllerSingleton.singleton)
        {
            ConcurrencyControllerSingleton.singleton = new ConcurrencyControllerSingleton();
        }

        if(ConcurrencyControllerSingleton.singleton.isDisposed)
            return null;


        return ConcurrencyControllerSingleton.singleton;
    }

    public void Dispose()
    {
        if(this.isDisposed)
            return;

        this.isDisposed = true;
    }
}
