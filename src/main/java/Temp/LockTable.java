package Temp;

import Utility.DEV_ID;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:02 AM
 */
public class LockTable
{
    public Map<DEV_ID, List<Routine>> lockTable;

    public int CURRENT_TIME;

    private static int ROUTINE_ID;

    public LockTable(List<DEV_ID> devIDlist)
    {
        this.lockTable = new HashMap<>();
        this.CURRENT_TIME = 0;
        this.ROUTINE_ID = 0;

        for(DEV_ID devID : devIDlist)
        {
            this.lockTable.put(devID, new ArrayList<>());
        }
    }

    public int getUniqueRtnID()
    {
        return LockTable.ROUTINE_ID++;
    }

    /*
    public void generateTestLockTable()
    {
        Routine rtn;
        Command cmd;

        rtn = new Routine(1);
        cmd = new Command(DEV_ID.FAN, 1);
        cmd.startTime = 0;
        rtn.addCommand(cmd);
        this.lockTable.get(DEV_ID.FAN).add(rtn);

        rtn = new Routine(2);
        cmd = new Command(DEV_ID.FAN, 3);
        cmd.startTime = 5;
        rtn.addCommand(cmd);
        this.lockTable.get(DEV_ID.FAN).add(rtn);
    }
*/
    @Override
    public String toString()
    {
        String str = "";

        for(Map.Entry<DEV_ID, List<Routine>> entry : this.lockTable.entrySet())
        {
            DEV_ID devID = entry.getKey();
            str += "\n " + devID.name() + " : ";

            for(Routine rtn : this.lockTable.get(devID))
            {
                str += "[<R" + rtn.ID + "|C"+ rtn.getCommandIndex(devID) +">:" + rtn.lockStartTime(devID) + ":";
                str += rtn.lockStartTime(devID) + rtn.lockDuration(devID) + ") ";
            }
        }

        return str;
    }

    public void register(Routine rtn, int currentTime)
    {
        rtn.ID = getUniqueRtnID();

        Set<DEV_ID> devIDset = new HashSet<>();
        for(Command cmd : rtn.commandList)
        {
            if(devIDset.contains(cmd.devID))
            {
                System.out.println("a routine should use a device only once!");
                assert(false);
                return;
            }
            else
            {
                if( !this.lockTable.keySet().contains(cmd.devID))
                {
                    System.out.println(cmd.devID + " is used in routine, but not registered in the lock table");
                    assert(false);
                    return;
                }
                devIDset.add(cmd.devID);
            }
        }

        this.insertRecursively(rtn, 0, currentTime, new HashSet<>(), new HashSet<>());
    }

    private boolean isNoOverlap(Set<Integer> set1, Set<Integer> set2)
    {
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection.isEmpty();
    }

    private Set<Integer> getPreSet(DEV_ID devID, int index)
    {// pre set should be the set of routine ids  0<= preset < index

        Set<Integer> preSet = new HashSet<>();

        for(int idx = 0 ; idx < index ; ++idx)
        {
            assert(idx < this.lockTable.get(devID).size());
            preSet.add(this.lockTable.get(devID).get(idx).ID);
        }

        return preSet;
    }

    private Set<Integer> getPostSet(DEV_ID devID, int index)
    {// post set should be the set of routine ids index <= postSet < end of list

        Set<Integer> postSet = new HashSet<>();

        for(int idx = index ; idx < this.lockTable.get(devID).size() ; ++idx)
        {
            postSet.add(this.lockTable.get(devID).get(idx).ID);
        }

        return postSet;
    }

    public boolean insertRecursively(Routine rtn, int commandIdx, int insertionStartTime, Set<Integer> _preSet, Set<Integer> _postSet)
    {
        assert(this.isNoOverlap(_preSet, _postSet));

        if(rtn.commandList.size() == commandIdx)
            return true;

        DEV_ID devID = rtn.getDevID(commandIdx);
        int commandStartTime = insertionStartTime;
        int cmdDuration = rtn.commandList.get(commandIdx).duration;

        while(true)
        {
            CmdInsertionData cmdInsertionData = getLockTableEmptyPlaceIndex(devID, commandStartTime, cmdDuration );
            commandStartTime = cmdInsertionData.cmdStartTime; // the commandStartTime might drift from "insertionStartTime"
            int lockTableInsertionIndex = cmdInsertionData.cmdInsertIndex;

            Set<Integer> preSet = new HashSet<>(_preSet);
            preSet.addAll(getPreSet( devID ,lockTableInsertionIndex));

            Set<Integer> postSet = new HashSet<>(_postSet);
            postSet.addAll(getPostSet(devID, lockTableInsertionIndex));

            if(isNoOverlap(preSet, postSet))
            {
                int nextCmdMinimumStartTime = commandStartTime + rtn.commandList.get(commandIdx).duration;
                boolean deepDive = insertRecursively(rtn, commandIdx + 1, nextCmdMinimumStartTime, preSet, postSet);/// call recursion

                if(deepDive)
                {
                    if((0 == commandIdx) && 1 < rtn.commandList.size())
                    {
                        int secondCmdStartTime = rtn.commandList.get(1).startTime;
                        int nextLockStartTime = Integer.MAX_VALUE;
                        if(lockTableInsertionIndex != lockTable.get(devID).size())
                        {
                            nextLockStartTime = this.lockTable.get(devID).get(lockTableInsertionIndex).lockStartTime(devID);
                        }

                        int earliestEndTime = Math.min(secondCmdStartTime, nextLockStartTime);

                        rtn.commandList.get(commandIdx).startTime = (earliestEndTime - rtn.commandList.get(commandIdx).duration); // set command Start Time
                    }
                    else
                    {
                        rtn.commandList.get(commandIdx).startTime = commandStartTime; // set command Start Time
                    }

                    this.lockTable.get(devID).add(lockTableInsertionIndex, rtn); // insert in the list
                    return true;
                }

            }

            if(lockTableInsertionIndex == lockTable.get(devID).size())
            {// already reached at the last, but still could not insert... return false
                return false;
            }
            else
            {
                commandStartTime = lockTable.get(devID).get(lockTableInsertionIndex).lockEndTime(devID);
            }

        }

    }


    public CmdInsertionData getLockTableEmptyPlaceIndex(DEV_ID _devID, int _scanStartTime, int _targetCmdDuration)
    {
        int index;
        int scanStartTime = _scanStartTime;

        for(index = 0 ; index < lockTable.get(_devID).size() ; ++index)
        {
            int cmdStartTime = lockTable.get(_devID).get(index).lockStartTime(_devID);
            int cmdEndTime = lockTable.get(_devID).get(index).lockEndTime(_devID);

            assert(cmdStartTime < cmdEndTime);

            if(cmdEndTime < scanStartTime)
            {
                continue;
            }
            else
            {
                if(cmdStartTime < scanStartTime)
                {// overlap with the scan line
                    scanStartTime = cmdEndTime; // shift the scan line
                    continue;
                }
                else
                {
                    int emptySlot = cmdStartTime - scanStartTime;

                    if( _targetCmdDuration <= emptySlot)
                    {
                        return new CmdInsertionData(scanStartTime, index);
                    }
                    else
                    {
                        scanStartTime = cmdEndTime; // shift the scan line
                    }

                }
            }

        }

        return new CmdInsertionData(scanStartTime, index);
    }

    public class CmdInsertionData
    {
        int cmdStartTime;
        int cmdInsertIndex;
        public CmdInsertionData(int _cmdStartTime, int _cmdInsertIndex)
        {
            this.cmdStartTime = _cmdStartTime;
            this.cmdInsertIndex = _cmdInsertIndex;
        }

        @Override
        public String toString()
        {
            String str = " cmdStartTime = " + this.cmdStartTime + " | cmdInsertIndex = " + this.cmdInsertIndex;
            return str;
        }
    }

}
