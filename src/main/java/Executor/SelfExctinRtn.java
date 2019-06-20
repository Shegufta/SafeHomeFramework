package Executor;

import CentralController.Controller;
import Utility.Command;
import Utility.DEV_STATUS;
import Utility.NextStep;
import Utility.Routine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
public class SelfExctinRtn
{
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;
    private List<Command> commandList;
    private Routine routine;
    public int uniqueRoutineID;
    private boolean isDisposed;
    public boolean isStarted;
    public boolean handledByConcurrencyController;

    //private int currentlyExecutingCmdIdx;
    private int successfullyExecutedCmdIdx;

    //private boolean isLongCmdEndPending;
    //private DEV_STATUS longRunningEndingStatus;
    Command currentCommand;
    ExecutorSingleton parentExecutor;


    public SelfExctinRtn(Routine _routine, ExecutorSingleton _parentExecutor)
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
        //this.isLongCmdEndPending = false;
        //this.longRunningEndingStatus = DEV_STATUS.NOT_INITIALIZED;
        this.successfullyExecutedCmdIdx = -1;

        this.ScheduleExecutor(0); // reschedule immediately
    }

    private void Execute()
    {
        if(this.isDisposed)
            return;

        //int successExecutedCmdIdx = this.currentlyExecutingCmdIdx;
        NextStep nextStep = this.parentExecutor.getNextStep(this.uniqueRoutineID, this.successfullyExecutedCmdIdx);

        int pauseOrHaultIntervalMS = -1;

        if(nextStep.nextCommandIndex != NextStep.HALT)
        {
            assert(nextStep.nextCommandIndex < this.commandList.size());

            assert ((this.successfullyExecutedCmdIdx + 1) == nextStep.nextCommandIndex); //TODO: as "NextIndex = 1 + current index" is obvious. I think, we can simplify this logic

            int currentlyExecutingCmdIdx = nextStep.nextCommandIndex;
            boolean isLastCommand = ( currentlyExecutingCmdIdx == (this.commandList.size() - 1) ); // zero indexing !

            this.currentCommand = this.commandList.get(currentlyExecutingCmdIdx); // get the command to be executed

            if(this.currentCommand.devName.equals(Controller.DEV_NAME_DUMMY))
            {   // wait command
                if (isLastCommand)
                {// ignore the last wait command
                    System.out.println("Ignoring the last wait command in Routine: " + this.uniqueRoutineID);
                    this.EndRoutineExecution();
                }
                else
                {
                    pauseOrHaultIntervalMS = currentCommand.durationMilliSec; // get the waiting interval. This pause time will come as a part of the routine (user input)
                }
            }
            else
            {
                this.sendCommandToLowerLayer(this.currentCommand.devName, this.currentCommand.desiredStatus); //Execute command

                if( isLastCommand )
                {
                    this.EndRoutineExecution(); // The last command has been executed... no need to reschedule... call the finish-routine command.
                }
                else
                {
                    this.ScheduleExecutor(0); // there are more commands... reschedule the Execution immediately
                }
            }

            this.successfullyExecutedCmdIdx = currentlyExecutingCmdIdx; // update successfully executed command idx
        }
        else
        {
            pauseOrHaultIntervalMS = nextStep.waitTimeMilliSecond; // this pause will be provided by the concurrency controller (calculated automatically)
        }

        if(0 < pauseOrHaultIntervalMS)
        {
            System.out.println("Routine : " + this.uniqueRoutineID + " | pausing execution for " + pauseOrHaultIntervalMS + " ms");
            this.ScheduleExecutor(pauseOrHaultIntervalMS); // reschedule after that interval
        }

    }

    private void ScheduleExecutor(int scheduleIntervalInMilliSec)
    {
        this.scheduledFuture = this.scheduledExecutorService.schedule(
                ()-> {this.Execute();},
                scheduleIntervalInMilliSec,
                TimeUnit.MILLISECONDS
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
}
