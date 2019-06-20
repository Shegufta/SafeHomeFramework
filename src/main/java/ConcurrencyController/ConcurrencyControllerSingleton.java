package ConcurrencyController;

import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.*;

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
    private LockTable globalLockTable;

    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;

    private ConcurrencyControllerSingleton()
    {
        this.isDisposed = false;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
    }

    public void initDeviceList(final Set<DEV_ID> devIDSet)
    {
        this.globalLockTable = new LockTable(devIDSet);
    }/// SBA: new API

    public void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        this.globalLockTable.registerRoutine(newRoutine);
    }

    public void commitRoutine(int committedRoutineID)
    {
        this.globalLockTable.commitRoutine(committedRoutineID);
    }

    public void commandFinishes()
    {
        this.scheduleCheckForAvailableLockAndNotify();
    }

    public void scheduleCheckForAvailableLockAndNotify()
    {
        this.ScheduleExecutor(0); // schedule immediately
    }

    private void checkForAvailableLockAndNotify()
    {
        this.globalLockTable.allocateAvailableLocksAndNotify();
    }

    //////////////////////////////////////////////////////////////////////////



    private synchronized void ScheduleExecutor(int scheduleIntervalInMilliSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.checkForAvailableLockAndNotify();},
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
