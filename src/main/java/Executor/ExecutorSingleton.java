package Executor;

import ConcurrencyController.ConcurrencyControllerSingleton;
import Utility.NextStep;
import Utility.Routine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/4/2019
 * @time 3:47 PM
 */
public class ExecutorSingleton
{
    private static ExecutorSingleton singleton;

    private Map<Integer, SelfExecutingRoutine> routineID_selfExcRtnMap;
    //private ScheduledFuture<?> scheduledFuture;
    //private ScheduledExecutorService scheduledExecutorService;

    private ExecutorSingleton()
    {
        this.routineID_selfExcRtnMap = new HashMap<>();
        //this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
    }

    public synchronized void ReceiveRoutine(Routine _routine)
    {
        System.out.println("Executor received Routine : " + _routine.uniqueRoutineID + "\n");
        int routineID = _routine.uniqueRoutineID;
        assert(!this.routineID_selfExcRtnMap.containsKey(routineID));

        this.routineID_selfExcRtnMap.put(routineID, new SelfExecutingRoutine(_routine, this));

        ConcurrencyControllerSingleton.getInstance().RegisterRoutine(_routine);

        ////////this.ScheduleExecutor(0); // schedule executor
    }

    /**
     * Called from ConcurrencyController
     * @param readyRoutineIDList
     */
    public synchronized void ExecuteRoutines(List<Integer> readyRoutineIDList)
    {
        for(int routineID : readyRoutineIDList)
        {
            assert(this.routineID_selfExcRtnMap.containsKey(routineID));
            assert(!this.routineID_selfExcRtnMap.get(routineID).isStarted);

            this.routineID_selfExcRtnMap.get(routineID).StartRoutineExecution();
        }
    }

    public NextStep getNextStep(int routineID, int successExecutedCmdIdx)
    {
        return ConcurrencyControllerSingleton.getInstance().getNextStep(routineID, successExecutedCmdIdx);
    }

    /*
    private synchronized void checkAndExecuteRoutine()
    {
        List<Integer> readyRoutineIDList = ConcurrencyControllerSingleton.getInstance().getPrepareToExecuteRoutineIDlist();

        for(int routineID : readyRoutineIDList)
        {
            assert(this.routineID_selfExcRtnMap.containsKey(routineID));

            this.routineID_selfExcRtnMap.get(routineID).StartRoutineExecution();
        }
    }
    */

    public synchronized void EndRoutineExecution(int _routineID)
    {
        assert(this.routineID_selfExcRtnMap.containsKey(_routineID));

        ConcurrencyControllerSingleton.getInstance().FinishRoutineExecution(_routineID);

        this.routineID_selfExcRtnMap.remove(_routineID); // remove the routine

        //this.ScheduleExecutor(0); // schedule executor
    }

    public static synchronized ExecutorSingleton getInstance()
    {
        if(null == ExecutorSingleton.singleton)
        {
            ExecutorSingleton.singleton = new ExecutorSingleton();
        }

//        if(ExecutorSingleton.singleton.isDisposed)
//            return null;

        return ExecutorSingleton.singleton;
    }


    /*
    private synchronized void ScheduleExecutor(int scheduleIntervalInSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.checkAndExecuteRoutine();},
                scheduleIntervalInSec,
                TimeUnit.MILLISECONDS
        ); // reschedule
    }
    */
}
