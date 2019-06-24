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

    private static long startTimeNanoSec;

    private static String TAGstart;
    private static String TAGclassName;

    private ConcurrencyControllerSingleton()
    {
        this.isDisposed = false;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
        System.nanoTime(); // dummy call... need it to increase the accuracy
        System.nanoTime(); // dummy call... need it to increase the accuracy
        ConcurrencyControllerSingleton.startTimeNanoSec = System.nanoTime();

        ConcurrencyControllerSingleton.TAGstart = "***";
        ConcurrencyControllerSingleton.TAGclassName = this.getClass().getSimpleName();

    }

    private String TAGaddThrdTime(final String TAG)
    {
        final int elapsedTimeMS = (int)(ConcurrencyControllerSingleton.getInstance().getElapsedTimeNanoSec()/1000000);
        final String threadName = Thread.currentThread().getName();

        return TAG +" | ThrdName = " + threadName + " | MS = " + elapsedTimeMS + " | ";
    }

    public static synchronized int getElapsedTimeNanoSec()
    {
        return (int)(System.nanoTime() - ConcurrencyControllerSingleton.startTimeNanoSec);
    }

    public void initDeviceList(final Set<DEV_ID> devIDSet)
    {
        this.globalLockTable = new LockTable(devIDSet);
    }

    public void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        System.out.println("\t|||||| BEFORE registering " + newRoutine);
        this.globalLockTable.printGlobalLockTable();

        this.globalLockTable.registerRoutine(newRoutine);

        System.out.println("\t|||||| AFTER registering " + newRoutine);
        this.globalLockTable.printGlobalLockTable();
    }

    public void commitRoutine(int committedRoutineID)
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        //System.out.println(this.TAGaddThrdTime(TAG) + " committedRoutineID = " + committedRoutineID);
        //System.out.println("\t\t\t ******* COMMITING Routine " + committedRoutineID);

        System.out.println("\t|||||| BEFORE comitting routineID" + committedRoutineID);
        this.globalLockTable.printGlobalLockTable();

        this.globalLockTable.commitRoutine(committedRoutineID);

        System.out.println("\t|||||| AFTER comitting routineID" + committedRoutineID);
        this.globalLockTable.printGlobalLockTable();
    }

    public void commandFinishes(int routineID, Command finishedCmd)
    {
//        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
//        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;
//        System.out.println(this.TAGaddThrdTime(TAG));

        System.out.println("\t|||||| Command Finish : [Routine ID : " +  routineID + " | " + finishedCmd );
        this.globalLockTable.printGlobalLockTable();
        this.scheduleCheckForAvailableLockAndNotify();
    }

    public void scheduleCheckForAvailableLockAndNotify()
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;
        System.out.println(this.TAGaddThrdTime(TAG));

        this.ScheduleExecutor(0); // schedule immediately
    }

    private void checkForAvailableLockAndNotify()
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;
        System.out.println(this.TAGaddThrdTime(TAG));

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
