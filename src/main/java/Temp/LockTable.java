package Temp;

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
    private CONSISTENCY_TYPE consistencyType;

    private static int ROUTINE_ID;

    public LockTable(List<DEV_ID> devIDlist, CONSISTENCY_TYPE _consistencyType)
    {
        this.lockTable = new HashMap<>();
        this.CURRENT_TIME = 0;
        this.ROUTINE_ID = 0;
        this.consistencyType = _consistencyType;

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

        //for(Map.Entry<DEV_ID, List<Routine>> entry : this.lockTable.entrySet())
        for(DEV_ID devID : DEV_ID.values())
        {// print the rows alphabetically
            if(!this.lockTable.containsKey(devID))
                continue;
            //DEV_ID devID = entry.getKey();
            str += "\n " + devID.name() + " : ";

            for(Routine rtn : this.lockTable.get(devID))
            {
                str += "[<R" + rtn.ID + "|C"+ rtn.getCommandIndex(devID) +">:" + rtn.lockStartTime(devID) + ":";
                str += rtn.lockStartTime(devID) + rtn.lockDuration(devID) + ") ";
            }
        }

        return str;
    }


    public class LazySchedulingHelper
    {
        public DEV_ID devID;
        public boolean isLocked;
        public int lockReleaseTime;

        public LazySchedulingHelper(DEV_ID _devID)
        {
            this.devID = _devID;
            this.isLocked = false;
        }
    }

    private int lazy_ReleaseEarliestCommandLock(Map<DEV_ID, LazySchedulingHelper> lockTableHelper, int simulationStartTime)
    {
        int earliestLockReleaseTime = Integer.MAX_VALUE;
        DEV_ID earliestReleaseDevID = null;

        for(LazySchedulingHelper RShlpr : lockTableHelper.values())
        {
            if(RShlpr.isLocked)
            {
                if(RShlpr.lockReleaseTime < earliestLockReleaseTime)
                {
                    earliestLockReleaseTime = RShlpr.lockReleaseTime;
                    earliestReleaseDevID = RShlpr.devID;
                }
            }
        }

        if(earliestReleaseDevID != null)
        {
            lockTableHelper.get(earliestReleaseDevID).isLocked = false;
        }

        return (earliestLockReleaseTime == Integer.MAX_VALUE) ? simulationStartTime : earliestLockReleaseTime;
    }

    private boolean lazy_canAcquireAllLocks(Map<DEV_ID, LazySchedulingHelper> lockTableHelper, Routine rtn)
    {
        for(DEV_ID devId : rtn.getAllDevIDSet())
        {
            if(lockTableHelper.get(devId).isLocked)
                return false;
        }

        return true;
    }

    private void lazy_acquireAllLock(Map<DEV_ID, LazySchedulingHelper> lockTableHelper, Routine rtn)
    {
        for(Command cmd : rtn.commandList)
        {
            lockTableHelper.get(cmd.devID).isLocked = true;
            lockTableHelper.get(cmd.devID).lockReleaseTime = cmd.getCmdEndTime();
        }
    }

    private void lazyScheduling(List<Routine> rtnList, int currentTime)
    {
        Map<DEV_ID, LazySchedulingHelper> lockTableHelper = new HashMap<>();

        for(DEV_ID devID : this.lockTable.keySet())
        {
            lockTableHelper.put( devID, new LazySchedulingHelper(devID) );
        }

        List<Routine> rtnListCopy = new ArrayList<>(rtnList); // NOTE: do not change the rtnList

        while(!rtnListCopy.isEmpty())
        {
            int lockReleaseTime = lazy_ReleaseEarliestCommandLock(lockTableHelper, currentTime);

            List<Routine> scheduledRtnList = new ArrayList<>();

            for(int index = 0 ; index < rtnListCopy.size() ; index++)
            {
                if(lazy_canAcquireAllLocks( lockTableHelper , rtnListCopy.get(index)))
                {
                    rtnListCopy.get(index).ID = getUniqueRtnID();
                    rtnListCopy.get(index).registrationTime = currentTime;

                    registerRoutineFromExactTime(rtnListCopy.get(index), lockReleaseTime); // register in the lock table
                    lazy_acquireAllLock(lockTableHelper ,rtnListCopy.get(index));

                    scheduledRtnList.add(rtnListCopy.get(index));
                }
            }

            for(Routine rtn : scheduledRtnList)
            {
                rtnListCopy.remove(rtn);
            }
        }
    }


    public void register(List<Routine> rtnList, int currentTime)
    {
        if(this.consistencyType == CONSISTENCY_TYPE.LAZY)
        {

            lazyScheduling(rtnList, currentTime);
            return;
        }

        for(Routine rtn : rtnList)
        {
            this.register(rtn, currentTime);
        }
    }

    public void register(Routine rtn, int currentTime)
    {
        rtn.ID = getUniqueRtnID();
        rtn.registrationTime = currentTime;

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

        switch(this.consistencyType)
        {
            case STRONG:
            {
                this.registerStrong(rtn, currentTime);
                break;
            }
            case RELAXED_STRONG:
            {
                registerRelaxedStepByStep(rtn, currentTime);
                break;
            }
            case EVENTUAL:
            {
                this.insertRecursively(rtn, 0, currentTime, new HashSet<>(), new HashSet<>());
                break;
            }
            case WEAK:
            {
                this.registerWeak(rtn, currentTime);
                break;
            }
            default:
            {
                assert(false);
            }
        }
    }


    private void registerStrong(Routine rtn, int currentTime)
    {
        int routineMaxEndTime = currentTime;

        for(DEV_ID devId: this.lockTable.keySet())
        {
            for(Routine existingRtn : lockTable.get(devId))
            {
                int existingRtnEndTime = existingRtn.routineEndTime();

                if(routineMaxEndTime < existingRtnEndTime)
                    routineMaxEndTime = existingRtnEndTime;
            }
        }

        int routineStartTime = routineMaxEndTime;

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

    private void registerRelaxedStepByStep(Routine rtn, int currentTime)
    {
        int overlappintRtnMaxEndTime = currentTime;

        //for(DEV_ID devId: rtn.devSet)
        for(DEV_ID devId: rtn.devIdIsMustMap.keySet())
        {
            for(Routine existingRtn : lockTable.get(devId))
            {
                int existingRtnEndTime = existingRtn.routineEndTime();

                if(overlappintRtnMaxEndTime < existingRtnEndTime)
                    overlappintRtnMaxEndTime = existingRtnEndTime;
            }
        }

        int routineStartTime = overlappintRtnMaxEndTime;

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

    private void registerWeak(Routine rtn, int currentTime)
    {
        this.registerRoutineFromExactTime(rtn, currentTime);
    }

    private void registerRoutineFromExactTime(Routine rtn, int initialTime)
    {// commands will be registered without any gap
        int commandIdx = 0;
        rtn.commandList.get(commandIdx).startTime = initialTime;
        DEV_ID devID = rtn.getDevID(commandIdx);
        this.lockTable.get(devID).add(rtn); // insert in the list

        for(commandIdx = 1 ; commandIdx < rtn.commandList.size() ; ++commandIdx)
        {
            rtn.commandList.get(commandIdx).startTime = rtn.commandList.get(commandIdx - 1).getCmdEndTime();

            devID = rtn.getDevID(commandIdx);
            this.lockTable.get(devID).add(rtn); // insert in the list
        }
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
                    int startTime;
                    if((0 == commandIdx) && 1 < rtn.commandList.size())
                    {
                        int secondCmdStartTime = rtn.commandList.get(1).startTime;
                        int nextLockStartTime = Integer.MAX_VALUE;
                        if(lockTableInsertionIndex != lockTable.get(devID).size())
                        {
                            nextLockStartTime = this.lockTable.get(devID).get(lockTableInsertionIndex).lockStartTime(devID);
                        }

                        int earliestEndTime = Math.min(secondCmdStartTime, nextLockStartTime);

                        startTime = (earliestEndTime - rtn.commandList.get(commandIdx).duration); // set command Start Time
                    }
                    else
                    {
                        startTime = commandStartTime; // set command Start Time
                    }

                    rtn.commandList.get(commandIdx).startTime = startTime;
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
        boolean isPreLeaseAllowed = Temp.IS_PRE_LEASE_ALLOWED;
        boolean isPostLeaseAllowed = Temp.IS_POST_LEASE_ALLOWED;

        int index;
        int scanStartTime = _scanStartTime;

        for(index = 0 ; index < lockTable.get(_devID).size() ; ++index)
        {
            Routine currentRtnInLineage = lockTable.get(_devID).get(index);
            int cmdStartTime = currentRtnInLineage.lockStartTime(_devID);
            int cmdEndTime = currentRtnInLineage.lockEndTime(_devID);

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

                        if(!isPreLeaseAllowed)
                        {
                            // donot need to check if currentRtnInLineage is committed!
                            if(currentRtnInLineage.isCandidateCmdInsidePreLeaseZone(_devID, scanStartTime, _targetCmdDuration )) // SBA: Never allow something before another routine
                            //if(true)// SBA: but it is a temporary lease, right? so i think, its ok
                            {
                                scanStartTime = cmdEndTime; // shift the scan line after the command
                                continue;
                            }
                        }

                        if(!isPostLeaseAllowed && (0 < index))
                        {
                            // lineage=>   [rtn 1 c1] [r4 c9] [prevRtnInLineage cx] [r_currentCandidate, c_Candidate (this command will be inserted)] [currentRtnInLineage, c*].....
                            Routine prevRtnInLineage = lockTable.get(_devID).get(index - 1);
                            // check if prevRtnInLineage is committed...
                            if(!prevRtnInLineage.isCommittedByGivenTime(scanStartTime))
                            {// prevRtnInLineage not committed... now check if it the new command overlaps with prevRtnInLineage's postLeaseZone
                                if(prevRtnInLineage.isCandidateCmdInsidePostLeaseZone(_devID, scanStartTime, _targetCmdDuration ))
                                {
                                    scanStartTime = prevRtnInLineage.routineEndTime(); // shift the scan line after the command
                                    continue;
                                }
                            }
                        }

                        return new CmdInsertionData(scanStartTime, index);
                    }
                    else
                    {
                        scanStartTime = cmdEndTime; // shift the scan line
                    }

                }
            }

        }

        //dont need to check "pre" at this point... if code comes here, this should be the last command.
        // hence, consider the post-lease flag of this command's previous routine.
        if(!isPostLeaseAllowed && (0 < index))
        {
            // lineage=>   [rtn 1 c1] [r4 c9] [prevRtnInLineage cx] [r_currentCandidate, c_Candidate (this command will be inserted)] [currentRtnInLineage, c*].....
            Routine prevRtnInLineage = lockTable.get(_devID).get(index - 1);
            // check if prevRtnInLineage is committed...
            if(!prevRtnInLineage.isCommittedByGivenTime(scanStartTime))
            {// prevRtnInLineage not committed... now check if it the new command overlaps with prevRtnInLineage's postLeaseZone
                if(prevRtnInLineage.isCandidateCmdInsidePostLeaseZone(_devID, scanStartTime, _targetCmdDuration ))
                {
                    scanStartTime = prevRtnInLineage.routineEndTime(); // shift the scan line after the command
                }
            }
        }


        return new CmdInsertionData(scanStartTime, index);

        /*
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

        */
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
