package ConcurrencyController;

import Executor.SelfExecutingRoutine;
import Utility.Device;
import Utility.Routine;

import java.util.ArrayList;
import java.util.List;

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
    //Map<String, PerDevLockTracker> lockTable;
    private LockTable lockTable;

    //List<SelfExecutingRoutine> selfExecutingRoutineList;

    private ConcurrencyControllerSingleton()
    {
        this.isDisposed = false;
        //this.selfExecutingRoutineList = new ArrayList<>();
        this.lockTable = new LockTable();
    }

    public void InitDeviceList(List<Device> devList)
    {
        this.lockTable.initLockTable(devList);
    }

    public void RegisterRoutine(Routine _routine)
    {
        this.lockTable.registerRoutine(_routine);
    }

    public List<Integer> getPrepareToExecuteRoutineIDlist()
    {
        return this.lockTable.getPrepareToExecuteRoutineIDlist();
    }

    public void FinishRoutineExecution(Integer _routineID)
    {
        this.UnregisterRoutine(_routineID);
    }

    private void UnregisterRoutine(Integer _routineID)
    {
        this.lockTable.unregisterRoutine(_routineID);
    }

    public void CommandExecuted(SelfExecutingRoutine _selfExecutingRoutine, int finishedCommandIndex)
    {
        //TODO: we will use it for pre/post lease. for now leave it empty
    }




    /*
    private void startRoutineExecution(SelfExecutingRoutine _selfExecutingRoutine)
    {
        _selfExecutingRoutine.StartRoutineExecution();
    }
    */






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
