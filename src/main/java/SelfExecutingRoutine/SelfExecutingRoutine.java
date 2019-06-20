package SelfExecutingRoutine;

import ConcurrencyController.ConcurrencyControllerSingleton;
import Utility.Command;
import Utility.DEV_ID;
import Utility.DEV_LOCK;
import Utility.DEV_STATUS;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/18/2019
 * @time 2:42 PM
 */
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
    public Map<DEV_ID, DevLockStatusCmdChainID> lockTable;
    public Boolean isStarted;// SBA: do not use "boolean", use "Boolean"... this object is used to thread synchronization
    public boolean isDisposed ;
    public int routineID;
    public Set<Integer> preRoutineSet;
    public Set<Integer> postRoutineSet;

    public SelfExecutingRoutine()
    {
        this.cmdChainList = Collections.synchronizedList(new ArrayList());
        this.lockTable = new ConcurrentHashMap<>();
        this.isStarted = false;
        this.isDisposed = false;
        this.preRoutineSet = new HashSet<>();
        this.postRoutineSet = new HashSet<>();
    }

    public synchronized DEV_STATUS getLastSuccessfullySetDevStatus(DEV_ID _devID)
    {
        return this.lockTable.get(_devID).getLastSuccessfullySetDevStatus();
    }

    public synchronized DEV_LOCK getLockStatus(DEV_ID _devID)
    {
        assert(this.lockTable.containsKey(_devID));

        return this.lockTable.get(_devID).dev_lock;
    }

    public synchronized void setLockStatus(DEV_ID devID, DEV_LOCK devLock)
    {
        assert(this.lockTable.containsKey(devID));

        System.out.println("\t\t" + devID.name() + " : lock status > " + devLock.name());

        this.lockTable.get(devID).setDevLock(devLock);
    }

    public void notifyToCheckLockInRelevantCommandChain(DEV_ID _devID)
    {
        assert(this.lockTable.containsKey(_devID));

        int commandChainIndex = this.lockTable.get(_devID).commandChainIndex;

        SelfExecutingCmdChain selfExecutingCmdChain = this.cmdChainList.get(commandChainIndex);

        if(!selfExecutingCmdChain.isFinished)
        {
            synchronized (selfExecutingCmdChain)
            {
                selfExecutingCmdChain.notify();
            }
        }
    }

    public void notifyToCheckLockInAllCmdChains()
    {
        System.out.println(System.currentTimeMillis() + " : notify");
        for(SelfExecutingCmdChain selfExecutingCmdChain : this.cmdChainList)
        {
            if(!selfExecutingCmdChain.isFinished)
            {
                synchronized (selfExecutingCmdChain)
                {
                    selfExecutingCmdChain.notify();
                }
            }
        }
    }

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


    public synchronized void reportCommandCompletion(Command _completedCommand, boolean _isDevUsedInFuture, boolean _wasTheLastCommand)
    {
        //System.out.println("\t\tCommand executed...");

        DEV_ID devID = _completedCommand.devID;
        DEV_STATUS successfulStatus = _completedCommand.desiredStatus;

        if(devID != DEV_ID.DUMMY_WAIT)
        {
            this.lockTable.get(devID).setLastSuccessfullySetDevStatus(successfulStatus);

            if(!_isDevUsedInFuture)
            {
                this.setLockStatus(devID, DEV_LOCK.RELEASED);
            }
            else
                System.out.println("\t\t\t%%%% " + devID.name() + " will be used in future.... do not release the lock");
        }
        else
            System.out.println("\t\t\t %%%%% DUMMY_WAIT command end...");


        if(_wasTheLastCommand && this.isRoutineFinished() )
        {
            assert(!_isDevUsedInFuture);

            ConcurrencyControllerSingleton.getInstance().commitRoutine(this.routineID);
        }
        else
        {
            ConcurrencyControllerSingleton.getInstance().commandFinishes();
        }
    }

    private synchronized boolean isRoutineFinished()
    {
        for(SelfExecutingCmdChain cmdChain : cmdChainList)
        {
            if(!cmdChain.isFinished)
                return false;
        }

        return true;
    }

    public void startExecution()
    {
        synchronized(isStarted)
        {
            if(this.isStarted)
                return;

            this.isStarted = true;
        }

        for(DEV_ID dev_id : lockTable.keySet())
        {// Locks have been acquired...
            this.setLockStatus(dev_id, DEV_LOCK.ACQUIRED);
        }

        for(SelfExecutingCmdChain selfExecutingCmdChain : this.cmdChainList)
        {
            selfExecutingCmdChain.Start();
        }
    }

    public Set<DEV_ID> getAllTouchedDevID()
    {
        return lockTable.keySet();
    }

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
            assert(!lockTable.containsKey(devID));

            lockTable.put(devID, new DevLockStatusCmdChainID(devID, newCmdChain.cmdChainIndx));
        }
    }

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
