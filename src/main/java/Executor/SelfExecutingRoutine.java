package Executor;

import Utility.Command;
import Utility.DEV_STATUS;
import Utility.Routine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
 * @time 5:14 PM
 */
public class SelfExecutingRoutine
{
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;
    private List<Command> commandList;
    private Routine routine;
    public int uniqueRoutineID;
    private boolean isDisposed;
    private boolean isStarted;
    public boolean handledByConcurrencyController;

    private int currentlyExecutingCmdIdx;

    private boolean isLongCmdEndPending;
    private DEV_STATUS longRunningEndingStatus;
    Command currentCommand;
    Executor parentExecutor;


    public SelfExecutingRoutine(Routine _routine, Executor _parentExecutor)
    {
        //this.commandList = new ArrayList<>();
        this.routine = _routine;
        this.uniqueRoutineID = this.routine.uniqueRoutineID;
        this.commandList = this.routine.commandList;
        this.isDisposed = false;
        this.isStarted = false;
        this.parentExecutor = _parentExecutor;
        //this.handledByConcurrencyController = false;
    }

    public void StartRoutineExecution()
    {
        if(isStarted)
            return;

        this.isStarted = true;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // init the executor
        this.isLongCmdEndPending = false;
        this.longRunningEndingStatus = DEV_STATUS.NOT_INITIALIZED;
        this.currentlyExecutingCmdIdx = 0;

        this.ScheduleExecutor(0); // reschedule immediately
    }

    private void Execute()
    {
        if(this.isDisposed)
            return;

        if( this.commandList.size() <= currentlyExecutingCmdIdx)
        {
            this.EndRoutineExecution();
            return;
        }

        if(!this.isLongCmdEndPending)
        {
            this.currentCommand = this.commandList.get(this.currentlyExecutingCmdIdx);
            this.isLongCmdEndPending = this.currentCommand.isLongCommand();
            int rescheduleIntervalInSec;

            if(this.isLongCmdEndPending)
            {// save the current state and turn the device back to this state once the long-running command ends
                this.longRunningEndingStatus = this.getCurrentDevStatus(this.currentCommand);
                rescheduleIntervalInSec = this.currentCommand.durationSecond;
            }
            else
            {
                this.longRunningEndingStatus = DEV_STATUS.NOT_INITIALIZED;
                rescheduleIntervalInSec = 0;
                currentlyExecutingCmdIdx++;
            }

            this.sendCommandToLowerLayer(this.currentCommand.devName, this.currentCommand.desiredStatus); //Execute command

            this.ScheduleExecutor(rescheduleIntervalInSec); // reschedule

        }
        else
        {// execute the end part of the long command
            this.isLongCmdEndPending = false; // reset the flag

            this.sendCommandToLowerLayer(this.currentCommand.devName, this.longRunningEndingStatus); //Execute command

            this.longRunningEndingStatus = DEV_STATUS.NOT_INITIALIZED;
            currentlyExecutingCmdIdx++;

            this.ScheduleExecutor(0); // reschedule immediately
        }
    }

    private void ScheduleExecutor(int scheduleIntervalInSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.Execute();},
                scheduleIntervalInSec,
                TimeUnit.SECONDS
        ); // reschedule
    }

    private void sendCommandToLowerLayer(String _deviceName, DEV_STATUS _desiredStatus)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println( dateFormat.format(date) + " > Routine : " + uniqueRoutineID+ " | Dev : " + _deviceName + " | set status to : " + _desiredStatus.name());
    }

    public void EndRoutineExecution()
    {
        this.scheduledFuture.cancel(false);
        this.scheduledExecutorService.shutdownNow();

        this.parentExecutor.EndRoutineExecution(this.uniqueRoutineID);
    }

    public DEV_STATUS getCurrentDevStatus(Command cmd)
    {// TODO: get the current dev status from DAG
        return DEV_STATUS.OFF; // TODO: remove this fixed code
    }

    public void Dispose()
    {
        //TODO: safe remove from DAG
        //TODO: safe remove from Concurrency Controller
        if(this.isDisposed)
            return;

        this.isDisposed = true;
        this.EndRoutineExecution();
    }

    public static void main(String[] args)
    {
        /*
        System.out.println("calling R1");
        SelfExecutingRoutine selfExecutingRoutine1 = new SelfExecutingRoutine(generateRoutine1());
        selfExecutingRoutine1.StartRoutineExecution();

        System.out.println("calling R2");
        SelfExecutingRoutine selfExecutingRoutine2 = new SelfExecutingRoutine(generateRoutine2());
        selfExecutingRoutine2.StartRoutineExecution();

        System.out.println("---------------------");
        */

        /*
        Map<Integer, Integer> tempMap = new HashMap<>();

        int key = 1;
        tempMap.put(key,key);

        key = 2;
        tempMap.put(key,key);

        key = 3;
        tempMap.put(key,key);

        for(Map.Entry<Integer, Integer> entry: tempMap.entrySet())
        {
            System.out.println("Key = " + entry.getKey() + " : value = " + entry.getValue() );
        }

        key = 30;
        tempMap.remove(key);

        for(Map.Entry<Integer, Integer> entry: tempMap.entrySet())
        {
            System.out.println("Key = " + entry.getKey() + " : value = " + entry.getValue() );
        }
        */

    }
/*
    public static Routine generateRoutine1()
    {// TODO: replace it with automated method
        Routine routine1 = new Routine();
        routine1.addCommand( new Command("fan_R1", DEV_STATUS.ON, 0) );
        routine1.addCommand( new Command("oven_R1", DEV_STATUS.ON, (10)) );
        routine1.addCommand( new Command("fan2_R1", DEV_STATUS.ON, 0) );
        routine1.addCommand( new Command("fan3_R1", DEV_STATUS.ON, 0) );

        return routine1;
    }

    public static Routine generateRoutine2()
    {// TODO: replace it with automated method
        Routine routine2 = new Routine();
        routine2.addCommand( new Command("dev1_R2", DEV_STATUS.ON, 0) );
        routine2.addCommand( new Command("dev1_R2", DEV_STATUS.ON, (2)) );
        routine2.addCommand( new Command("dev1_R2", DEV_STATUS.ON, 2) );
        routine2.addCommand( new Command("dev1_R2", DEV_STATUS.ON, 2) );

        return routine2;
    }
    */



}
