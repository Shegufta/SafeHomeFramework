package ConcurrencyController;

import Executor.ExecutorSingleton;
import Executor.SelfExecutingRoutine;
import Utility.Device;
import Utility.NextStep;
import Utility.Routine;

import java.util.ArrayList;
import java.util.List;
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
        this.lockTable = new LockTable();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
    }

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
        List<Integer> routineToInitiateList = this.lockTable.getPrepareToExecuteRoutineIDlist();
        if(!routineToInitiateList.isEmpty())
        {
            ExecutorSingleton.getInstance().ExecuteRoutines(routineToInitiateList);
        }
    }

    private synchronized void ScheduleExecutor(int scheduleIntervalInMilliSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.checkForAvailableRoutines();},
                scheduleIntervalInMilliSec,
                TimeUnit.MILLISECONDS
        ); // reschedule
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
