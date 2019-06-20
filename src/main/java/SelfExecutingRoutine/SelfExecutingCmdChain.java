package SelfExecutingRoutine;

import Utility.Command;
import Utility.DEV_ID;
import Utility.DEV_LOCK;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/18/2019
 * @time 4:05 PM
 */
public class SelfExecutingCmdChain implements Runnable
{
    private Thread thread;
    private boolean isDisposed;
    public List<Command> commandChain;
    public Set<DEV_ID> devicesSet;
    public int cmdChainIndx;
    private SelfExecutingRoutine parentRtn;
    private DEV_ID currentDevice;
    public boolean isFinished;

    public DEV_ID getCurrentCmdChainHeadDev()
    {
        return this.currentDevice;
    }

    public SelfExecutingCmdChain(SelfExecutingRoutine _parentRtn, int _cmdChainID, List<Command> _commandChain)
    {
        this.parentRtn = _parentRtn;
        this.cmdChainIndx = _cmdChainID;
        this.commandChain = new ArrayList<>(_commandChain);
        this.devicesSet = new HashSet<>();
        this.currentDevice = DEV_ID.DEV_NOT_ASSIGNED;
        this.isFinished = false;

        for(Command cmd : this.commandChain)
        {
            if(cmd.devID == DEV_ID.DUMMY_WAIT)
                continue;

            this.devicesSet.add(cmd.devID);
        }
    }

    public synchronized void Start()
    {
        this.thread = new Thread(this);
        this.thread.start();
    }

    public boolean isDevUsedInFuture(int _currentCmdIndex)
    {
        for( int futureIdx = _currentCmdIndex + 1; futureIdx < this.commandChain.size() ; futureIdx++)
        {
            if(commandChain.get(futureIdx).devID == commandChain.get(_currentCmdIndex).devID)
                return true;
        }

        return false;
    }

    @Override
    public void run()
    {

        for( int currentCommandIdx = 0; !this.isDisposed && (currentCommandIdx < this.commandChain.size()) ; currentCommandIdx++)
        {
            Command currentCommand = commandChain.get(currentCommandIdx);
            this.currentDevice = currentCommand.devID;

            System.out.println("\t\t\t ==== Current Dev: " + this.currentDevice);

            if(this.currentDevice == DEV_ID.DUMMY_WAIT)
            {
                int sleepTimeMS = currentCommand.durationMilliSec;
                assert(0 < sleepTimeMS);

                while(!this.isDisposed && 0 < sleepTimeMS)
                {
                    long sleepStartTimeMS = System.currentTimeMillis();
                    try
                    {
                        System.out.println("\t\t\t ;;;;;; Command Level Sleep Start");
                        Thread.sleep(sleepTimeMS);
                        System.out.println("\t\t\t ;;;;;; Command Level Sleep END");
                        this.isFinished = (currentCommandIdx == (this.commandChain.size()-1));
                        this.parentRtn.reportCommandCompletion(currentCommand, false, this.isFinished);
                        break;
                        // SBA: make sure not to acquire any thread level locks....
                        // do not put inside Synchronized block
                        // In Java, Thread.sleep will NOT release the lock!
                        // https://howtodoinjava.com/java/multi-threading/sleep-vs-wait/
                    }
                    catch (InterruptedException ex)
                    {
                        System.out.println(ex.toString());

                        long sleepDuration = System.currentTimeMillis() - sleepStartTimeMS;
                        sleepTimeMS -= sleepDuration; // update remaining sleep time
                    }
                }
            }
            else
            {
                while(!this.isDisposed)
                {
                    DEV_LOCK devLock = this.parentRtn.getLockStatus(this.currentDevice);

                    System.out.println("\t\t\t\t @@@@ Dev Lock for  " + this.currentDevice.name() + " is " + devLock.name() );

                    assert(devLock != DEV_LOCK.RELEASED); // Once released, it should not search for the lock.

                    if(devLock == DEV_LOCK.EXECUTING)
                    {
                        System.out.println("\t\t\t\t @@@@ Execute" );

                        this.executeCommand(currentCommand);
                        boolean isDevUsedInFuture = this.isDevUsedInFuture(currentCommandIdx);
                        this.isFinished = (currentCommandIdx == (this.commandChain.size()-1));
                        this.parentRtn.reportCommandCompletion(currentCommand, isDevUsedInFuture, this.isFinished);
                        break;
                    }
                    else
                    {
                        try
                        {
                            System.out.println("\t\t\t************ Waiting for notification....");
                            synchronized (this)
                            { //SBA: apart from the wait(), do not put any logic inside this sync block!
                                this.wait();
                            }
                            System.out.println("\t\t\t************ Notification Received...");
                        }
                        catch (InterruptedException e)
                        {
                            if(this.isDisposed)
                            {
                                System.out.println("Disposed....");
                            }
                            else
                            {
                                System.out.println("Something is wrong... without Disposing, this Wait() state should not be interrupted!");
                                e.printStackTrace();
                            }

                            System.out.println("Interrupted");
                        }
                    }
                }

            }

        }

        System.out.println("End execution of this command chain : " + this);

    }

    public void executeCommand(Command cmd)
    {
        System.out.println("\t\t\t\t @@@ Set Device: " + cmd.devID + " to " + cmd.desiredStatus);
    }
    public synchronized void Dispose()
    {
        if(this.isDisposed)
            return;

        isDisposed = true;
        this.thread.interrupt();
    }

    @Override
    public String toString()
    {
        String str = "[";

        for(int I = 0 ; I < this.commandChain.size() ; ++I)
        {
            str += this.commandChain.get(I);

            if(I <= (this.commandChain.size() - 2) )
                str += ",";
        }
        str += "]";

        return str;
    }
}
