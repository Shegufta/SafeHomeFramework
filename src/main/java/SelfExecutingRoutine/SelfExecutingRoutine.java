/**
 * SelfExecutingRoutine for SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/18/2019
 * @time 2:42 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package SelfExecutingRoutine;

import ConcurrencyController.ConcurrencyControllerSingleton;
import LockTableManager.LockTableSingleton;
import Utility.Command;
import Utility.DEV_ID;
import Utility.DEV_LOCK;
import Utility.DEV_STATUS;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SelfExecutingRoutine
{
    public class DevLockStatusCmdChainID
    {
        public DEV_ID dev_id;
        public DEV_LOCK dev_lock;
        public int commandChainIndex;
        private DEV_STATUS lastSuccessfullySetDevStatus;

        public DevLockStatusCmdChainID(DEV_ID _dev_id, int _commandChainIndex)
        {
            this.dev_id = _dev_id;
            this.commandChainIndex = _commandChainIndex;
            this.dev_lock = DEV_LOCK.NOT_ACQUIRED;
            this.lastSuccessfullySetDevStatus = DEV_STATUS.NOT_INITIALIZED;
        }

        public void setDevLock(DEV_LOCK _devLock)
        {
            this.dev_lock = _devLock;
        }

        public void setLastSuccessfullySetDevStatus(DEV_STATUS _devStatus)
        {
            this.lastSuccessfullySetDevStatus = _devStatus;
        }

        public DEV_STATUS getLastSuccessfullySetDevStatus()
        {
            return this.lastSuccessfullySetDevStatus;
        }
    }

    public List<SelfExecutingCmdChain> cmdChainList;
    public Map<DEV_ID, DevLockStatusCmdChainID> routineLockTable;
    public boolean isStarted;
    public boolean isDisposed ;
    public int routineID;
    public Set<Integer> preRoutineSet;
    public Set<Integer> postRoutineSet;

    private static String TAGstart;
    private static String TAGclassName;

    public SelfExecutingRoutine()
    {
        this.cmdChainList = Collections.synchronizedList(new ArrayList());
        this.routineLockTable = new ConcurrentHashMap<>();
        this.isStarted = false;
        this.isDisposed = false;
        this.preRoutineSet = new HashSet<>();
        this.postRoutineSet = new HashSet<>();

        SelfExecutingRoutine.TAGstart = "@@@";
        SelfExecutingRoutine.TAGclassName = this.getClass().getSimpleName();
    }
    ///////////////////////////////////////////////////////////////////////////
    public void assignRoutineID(int _routineID)
    {
        this.routineID = _routineID;
    }
    ///////////////////////////////////////////////////////////////////////////
    public void addCmdChain(List<Command> _commandList)
    {
        assert(!this.isStarted);

        int newCmdChainIndex = this.cmdChainList.size();
        SelfExecutingCmdChain newCmdChain = new SelfExecutingCmdChain(this, newCmdChainIndex, _commandList);

        for(SelfExecutingCmdChain existingCmdChain : this.cmdChainList)
        {
            Set<DEV_ID> intersection = new HashSet<>(existingCmdChain.devicesSet);
            intersection.retainAll(newCmdChain.devicesSet);
            if(!intersection.isEmpty())
            {
                System.out.println("Error: inside a routine, two command chains should not use same devices!");
                System.out.println("New Command: " + newCmdChain);
                System.out.println("Existing Routine: " + this);
                assert(false);
            }
        }

        this.cmdChainList.add(newCmdChain.cmdChainIndx, newCmdChain);

        for(DEV_ID devID : newCmdChain.devicesSet)
        {
            assert(!routineLockTable.containsKey(devID));

            routineLockTable.put(devID, new DevLockStatusCmdChainID(devID, newCmdChain.cmdChainIndx));
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public synchronized void startExecution()
    {
        if(this.isStarted)
            return;

        this.isStarted = true;

        synchronized (LockTableSingleton.lockTableLockObject )
        {
            for(DEV_ID dev_id : routineLockTable.keySet())
            {// Locks have been acquired...
                this.setLockStatus(dev_id, DEV_LOCK.ACQUIRED);
            }
        }

        for(SelfExecutingCmdChain selfExecutingCmdChain : this.cmdChainList)
        {
            selfExecutingCmdChain.Start();
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public void reportCommandCompletion(Command _completedCommand, boolean _isDevUsedInFuture, boolean _wasTheLastCommand)
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        System.out.println("\n\t\t\t $$$$$$ RoutineID " + this.routineID + " : Command Done -> " + _completedCommand + "\n");

        DEV_ID devID = _completedCommand.devID;
        DEV_STATUS successfulStatus = _completedCommand.desiredStatus;

        if(devID != DEV_ID.DUMMY_WAIT)
        {
            synchronized (LockTableSingleton.lockTableLockObject)
            {
                this.routineLockTable.get(devID).setLastSuccessfullySetDevStatus(successfulStatus);

                if (!_isDevUsedInFuture)
                    this.setLockStatus(devID, DEV_LOCK.RELEASED);
            }
        }

        if(_wasTheLastCommand && this.isRoutineFinished() )
        {
            assert(!_isDevUsedInFuture);
            //System.out.println("\t\t" + this.TAGaddThrdTime(TAG) + " | TODO: commit Routine");
            LockTableSingleton.getInstance().commitRoutine(this.routineID);
        }
        else
        {
            //System.out.println("\t\t" + this.TAGaddThrdTime(TAG) + " | TODO: send command end signal");
            LockTableSingleton.getInstance().commandFinishes(this.routineID, _completedCommand);
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    private boolean isRoutineFinished()
    {
        for(SelfExecutingCmdChain cmdChain : cmdChainList)
        {
            if(!cmdChain.isFinished)
                return false;
        }

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////
    public void notifyToCheckLockInRelevantCommandChain(DEV_ID _devID)
    {

        assert(this.routineLockTable.containsKey(_devID));

        int commandChainIndex = this.routineLockTable.get(_devID).commandChainIndex;

        SelfExecutingCmdChain selfExecutingCmdChain = this.cmdChainList.get(commandChainIndex);

        synchronized (selfExecutingCmdChain)
        {
            if(!selfExecutingCmdChain.isFinished)
                selfExecutingCmdChain.notify();
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    public void clearPrePostRoutineSet()
    {
        this.preRoutineSet.clear();
        this.postRoutineSet.clear();
    }
    ///////////////////////////////////////////////////////////////////////////
    public synchronized boolean isWaitingForExecution(DEV_ID _devID)
    {
        for( SelfExecutingCmdChain cmdChain : this.cmdChainList)
        {
            System.out.println("isFinished = " + cmdChain.isFinished + " | what is the current dev = " + cmdChain.currentDevice);
            if(!cmdChain.isFinished && (cmdChain.currentDevice == _devID) )
                return true;
        }
        return false;
    }
    ///////////////////////////////////////////////////////////////////////////
    public DEV_STATUS getLastSuccessfullySetDevStatus(DEV_ID _devID)
    {
        synchronized (LockTableSingleton.lockTableLockObject)
        {
            return this.routineLockTable.get(_devID).getLastSuccessfullySetDevStatus();
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public  DEV_LOCK getLockStatus(DEV_ID _devID)
    {
        synchronized (LockTableSingleton.lockTableLockObject)
        {
            assert(this.routineLockTable.containsKey(_devID));
            return this.routineLockTable.get(_devID).dev_lock;
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public void setLockStatus(DEV_ID devID, DEV_LOCK devLock)
    {
        final String functionName = "." + new Throwable().getStackTrace()[0].getMethodName() + "()";
        final String TAG  =  this.TAGstart + " - "+ this.TAGclassName + functionName;

        System.out.println(this.TAGaddThrdTime(TAG) + " trying to enter synchronized(this.routineLockTable) | devID = "
        + devID.name()
        + " | devLock = " + devLock.name() );

        synchronized (LockTableSingleton.lockTableLockObject)
        {
            assert(this.routineLockTable.containsKey(devID));

            System.out.println("\t" + this.TAGaddThrdTime(TAG) + " INSIDE synchronized(this.routineLockTable) | devID = "
                    + devID.name()
                    + " | devLock = " + devLock.name() );

            this.routineLockTable.get(devID).setDevLock(devLock);
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public void Dispose()
    {
        if(this.isDisposed)
            return;

        this.isDisposed = true;

        System.out.println(System.currentTimeMillis() + " : Dispose");
        for(SelfExecutingCmdChain selfExecutingCmdChain : this.cmdChainList)
        {
            selfExecutingCmdChain.Dispose();
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    public Set<DEV_ID> getAllTouchedDevID()
    {
        return routineLockTable.keySet();
    }
    ///////////////////////////////////////////////////////////////////////////
    private String TAGaddThrdTime(final String TAG)
    {
        final int elapsedTimeMS = (int)(LockTableSingleton.getInstance().getElapsedTimeNanoSec()/1000000);
        final String threadName = Thread.currentThread().getName();

        return TAG +" | ThrdName = " + threadName + " | MS = " + elapsedTimeMS + " | ";
    }
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString()
    {
        String str = "[\n";

        for(SelfExecutingCmdChain existingCmdChain : this.cmdChainList)
        {
            str += existingCmdChain + "\n";
        }

        str += "]";

        return str;
    }
    ///////////////////////////////////////////////////////////////////////////

/**
    public static void main(String[] args)
    {
        List<Command> cmdChain = new ArrayList<>();

        cmdChain.add(new Command(DEV_ID.FAN, DEV_STATUS.ON, 0));
        cmdChain.add(new Command(DEV_ID.DUMMY_WAIT, DEV_STATUS.NOT_INITIALIZED, 5000));
        cmdChain.add(new Command(DEV_ID.LIGHT, DEV_STATUS.ON, 0));

        SelfExecutingRoutine selfExcRtn = new SelfExecutingRoutine();
        selfExcRtn.addCmdChain(cmdChain);
        selfExcRtn.startExecution();

        sleep(1000);
        selfExcRtn.notifyToCheckLockInAllCmdChains();

        sleep(2000);
        selfExcRtn.setLockStatus(DEV_ID.FAN, DEV_LOCK.EXECUTING);
        selfExcRtn.notifyToCheckLockInAllCmdChains();

        sleep(7000);
        selfExcRtn.notifyToCheckLockInAllCmdChains();

        sleep(4000);
        selfExcRtn.setLockStatus(DEV_ID.LIGHT, DEV_LOCK.EXECUTING);
        selfExcRtn.notifyToCheckLockInAllCmdChains();

//        sleep(4000);
//        selfExcRtn.setLockStatus(DEV_ID.LIGHT, DEV_LOCK.EXECUTING);
//        selfExcRtn.notifyToCheckLockInAllCmdChains();

        sleep(10000);
        selfExcRtn.Dispose();

    }

    public static void sleep(int sleepTimeMS)
    {
        try
        {
            System.out.println(System.currentTimeMillis() + "### : Sleeping for (ms): " + sleepTimeMS);
            Thread.sleep(sleepTimeMS);
            System.out.println(System.currentTimeMillis() + "### : sleep end...");
        }
        catch(Exception ex)
        {
            System.out.println(ex);
        }
    }
*/

}
