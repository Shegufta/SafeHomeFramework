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
    List<Routine> weakSchedulingSpecialLockTable = null;

    //public int CURRENT_TIME;
    public CONSISTENCY_TYPE consistencyType;

    //private static int ROUTINE_ID;

    public LockTable(List<DEV_ID> devIDlist, CONSISTENCY_TYPE _consistencyType)
    {
        this.lockTable = new HashMap<>();
        //this.CURRENT_TIME = 0;
        //this.ROUTINE_ID = 0;
        this.consistencyType = _consistencyType;

        for(DEV_ID devID : devIDlist)
        {
            this.lockTable.put(devID, new ArrayList<>());
        }
    }

    public List<Routine> getAllRoutineSet()
    {
        if(this.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            assert(weakSchedulingSpecialLockTable != null);
            return weakSchedulingSpecialLockTable;
        }
        else
        {
            List<Routine> allRoutineList = new ArrayList<>();

            for(List<Routine> lineage : lockTable.values())
            {
                for(Routine routine : lineage)
                {
                    if(!allRoutineList.contains(routine))
                        allRoutineList.add(routine);
                }
            }

            return allRoutineList;
        }
    }

//    public int getUniqueRtnID()
//    {
//        return LockTable.ROUTINE_ID++;
//    }

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
        String str = "------------------------------------------------------\n";
        str += "CONSISTENCY: " + this.consistencyType + "\n";
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
        str += "\n------------------------------------------------------\n";

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

    private int lazy_ReleaseEarliestCommandLock(Map<DEV_ID, LazySchedulingHelper> lockTableHelper, int simulationStartTime, List<Routine> _waitingList)
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
        { // search for another waiting routine that was registered before earliestLockReleaseTime
            // and can acquire all locks before current command ends
            for(Routine waitingRtn : _waitingList)
            {
                if(waitingRtn.registrationTime < earliestLockReleaseTime)
                {
                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRtn))
                    {
                        //canAnotherRoutineAcquireAllLocksBeforeThisCommandEnds = true;
                        int anotherRoutineCanStartAtTime = waitingRtn.registrationTime;
                        return anotherRoutineCanStartAtTime;
                    }
                }
                else
                    break;
            }


            // No other routine will start before this command, hence release this lock
            lockTableHelper.get(earliestReleaseDevID).isLocked = false; // release lock
        }

        return (earliestReleaseDevID == null) ? simulationStartTime : earliestLockReleaseTime;
    }

    private boolean isAllLockReleased(Map<DEV_ID, LazySchedulingHelper> lockTableHelper)
    {
        for(LazySchedulingHelper RShlpr : lockTableHelper.values())
        {
            if(RShlpr.isLocked)
            {
                return false;
            }
        }

        return true;
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

    private void lazyScheduling(final List<Routine> rtnList, int _simulationStartTime)
    {
        Map<DEV_ID, LazySchedulingHelper> lockTableHelper = new HashMap<>();

        for(DEV_ID devID : this.lockTable.keySet())
        {
            lockTableHelper.put( devID, new LazySchedulingHelper(devID) );
        }

        List<Routine> waitingList = new ArrayList<>(rtnList); // NOTE: do not change the rtnList
        Collections.sort(waitingList, new Routine());// sort based on arrival time. ties are broken by routine id. smaller id first

        while(!waitingList.isEmpty())
        {
            int lockReleaseTime = lazy_ReleaseEarliestCommandLock(lockTableHelper, _simulationStartTime, waitingList);
            boolean isAllLockReleased = this.isAllLockReleased(lockTableHelper);
            List<Routine> scheduledRtnList = new ArrayList<>();


            if(consistencyType == CONSISTENCY_TYPE.LAZY)
            {
                for(Routine waitingRoutine : waitingList)
                {
                    if( !isAllLockReleased &&(lockReleaseTime < waitingRoutine.registrationTime))
                        break;

                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRoutine))
                    {
                        int maxTime = Math.max(lockReleaseTime, waitingRoutine.registrationTime);

                        registerRoutineFromExactTime(waitingRoutine, maxTime); // register in the lock table
                        lazy_acquireAllLock(lockTableHelper ,waitingRoutine);

                        scheduledRtnList.add(waitingRoutine);
                    }
                }
            }
            else if(consistencyType == CONSISTENCY_TYPE.LAZY_FCFS)
            { // stop checking after first routine from the waiting list
                Routine firstWaitingRoutine = waitingList.get(0); // get the first routine

                if(!isAllLockReleased)
                {
                    for(Routine earlyComerNonConflictingRtn : waitingList)
                    {
                        if(lockReleaseTime < earlyComerNonConflictingRtn.registrationTime)
                            break;

                        if(lazy_canAcquireAllLocks( lockTableHelper , earlyComerNonConflictingRtn))
                        {
                            firstWaitingRoutine = earlyComerNonConflictingRtn;
                        }

                    }
                }

                if(lazy_canAcquireAllLocks( lockTableHelper , firstWaitingRoutine))
                {
                    int maxTime = Math.max(lockReleaseTime, firstWaitingRoutine.registrationTime);

                    registerRoutineFromExactTime(firstWaitingRoutine, maxTime); // register in the lock table
                    lazy_acquireAllLock(lockTableHelper ,firstWaitingRoutine);

                    scheduledRtnList.add(firstWaitingRoutine);
                }

            }
            else if(consistencyType == CONSISTENCY_TYPE.LAZY_PRIORITY)
            {
                List<Routine> candidateList = new ArrayList<>();

                for(Routine waitingRoutine : waitingList)
                {
                    if( !isAllLockReleased &&(lockReleaseTime < waitingRoutine.registrationTime))
                        break;

                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRoutine))
                    {
                        candidateList.add(waitingRoutine);
                    }
                }

                if(!candidateList.isEmpty())
                {
                    List<Routine> existingCandidateList = new ArrayList<>();
                    List<Integer> waitTimeList = new ArrayList<>(); // for routines, that arrive before or at "lockReleaseTime"

                    List<Routine> futureCandidateList = new ArrayList<>();
                    List<Integer> rightToCurreneScanLineArrivalIntervalList = new ArrayList<>(); // for routines, that arrive after "lockReleaseTime"

                    for (Routine candidateRtn : candidateList)
                    {
                        int waitTime = lockReleaseTime - candidateRtn.registrationTime;

                        if (0 <= waitTime)
                        {//for routines, that arrive before or at "lockReleaseTime"
                            waitTimeList.add(waitTime);
                            existingCandidateList.add(candidateRtn);
                        }
                        else
                        {// for routines, that arrive after "lockReleaseTime"
                            int arrivalInterval = candidateRtn.registrationTime - lockReleaseTime;
                            rightToCurreneScanLineArrivalIntervalList.add(arrivalInterval);
                            futureCandidateList.add(candidateRtn);
                        }
                    }

                    Routine candidateRoutine = null;

                    if(!waitTimeList.isEmpty())
                    {// select from left side of "lockReleaseTime"

                        Collections.sort(waitTimeList, Collections.reverseOrder()); // sort in descending order
                        int targetWaitTime = waitTimeList.get(0);

                        for (Routine rtn : existingCandidateList)
                        {
                            int waitTime = lockReleaseTime - rtn.registrationTime;

                            if (waitTime == targetWaitTime)
                            {
                                candidateRoutine = rtn;
                                break;
                            }
                        }
                    }
                    else
                    { // select from right side of "lockReleaseTime"
                        Collections.sort(rightToCurreneScanLineArrivalIntervalList); // sort in ascending order;
                        int targetArrivalTime = rightToCurreneScanLineArrivalIntervalList.get(0);

                        for (Routine rtn : futureCandidateList)
                        {
                            int arrivalInterval = rtn.registrationTime - lockReleaseTime;

                            if(targetArrivalTime == arrivalInterval)
                            {
                                candidateRoutine = rtn;
                                break;
                            }
                        }
                    }

                    int maxTime = Math.max(lockReleaseTime, candidateRoutine.registrationTime);

                    registerRoutineFromExactTime(candidateRoutine, maxTime); // register in the lock table
                    lazy_acquireAllLock(lockTableHelper ,candidateRoutine);

                    scheduledRtnList.add(candidateRoutine);
                }

            }
            else
            {
                System.out.println("\n\nERROR: LockTable.java: should not execute this line");
                assert(false);
                System.exit(1); // trigger if assertion is turned off!
            }


            for(Routine rtn : scheduledRtnList)
            {
                waitingList.remove(rtn);
            }
        }
    }



    //private void weakScheduling(List<Routine> rtnList, int _simulationStartTime)
    private void weakScheduling(List<Routine> rtnList)
    {
        //Collections.shuffle(rtnList); // shuffle the rtn list. in weak visibility, routine can be executed in any order.

        this.lockTable = null; // so that accidentally no one uses it while in WeakScheduling mode
        weakSchedulingSpecialLockTable = new ArrayList<>();

        for(int I = 0 ; I < rtnList.size() ; I++)
        {
            weakSchedulingSpecialLockTable.add(I, rtnList.get(I));

            //weakSchedulingSpecialLockTable.get(I).registrationTime = _simulationStartTime;
            //weakSchedulingSpecialLockTable.get(I).arrivalSequenceForWeakScheduling = I;

            assert(!weakSchedulingSpecialLockTable.get(I).commandList.isEmpty());

            //weakSchedulingSpecialLockTable.get(I).commandList.get(0).startTime = _simulationStartTime;
            weakSchedulingSpecialLockTable.get(I).commandList.get(0).startTime = weakSchedulingSpecialLockTable.get(I).registrationTime;

            for(int cmdIdx = 1 ; cmdIdx < weakSchedulingSpecialLockTable.get(I).commandList.size() ; cmdIdx++)
            {
                int prevCmdEndTime = weakSchedulingSpecialLockTable.get(I).commandList.get(cmdIdx-1).getCmdEndTime();
                weakSchedulingSpecialLockTable.get(I).commandList.get(cmdIdx).startTime = prevCmdEndTime;
            }
        }
    }


    public void register(List<Routine> rtnList, int _simulationStartTime)
    {
        if(this.consistencyType == CONSISTENCY_TYPE.LAZY
        || this.consistencyType == CONSISTENCY_TYPE.LAZY_FCFS
                || this.consistencyType == CONSISTENCY_TYPE.LAZY_PRIORITY
        )
        {
            lazyScheduling(rtnList, _simulationStartTime);
            return;
        }

        if(this.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            //this.weakScheduling(rtnList, _simulationStartTime);
            this.weakScheduling(rtnList);
            return;
        }

        for(Routine rtn : rtnList)
        {
            this.register(rtn, _simulationStartTime);
        }

//        if(this.consistencyType == CONSISTENCY_TYPE.EVENTUAL)
//        {
//            System.out.println(this);
//            System.out.println("\n calling System.exit(1) inside LockTable.java");
//            System.exit(1);
//        }
    }

    public void register(Routine rtn, int _simulationStartTime)
    {
        //rtn.registrationTime = _simulationStartTime;

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
                this.registerStrong(rtn, _simulationStartTime);
                break;
            }
            case RELAXED_STRONG:
            {
                registerRelaxedStepByStep(rtn, _simulationStartTime);
                break;
            }
            case EVENTUAL:
            {
                //this.insertRecursively(rtn, 0, _simulationStartTime, new HashSet<>(), new HashSet<>());
                this.insertRecursively(rtn, 0, rtn.registrationTime, new HashSet<>(), new HashSet<>());
                break;
            }
            default:
            {
                assert(false);
            }
        }
    }


    private void registerStrong(Routine rtn, int _simulationStartTime)
    {
        int registeredRtnMaxEndTime = _simulationStartTime;

        for(DEV_ID devId: this.lockTable.keySet())
        {
            for(Routine existingRtn : lockTable.get(devId))
            {
                int existingRtnEndTime = existingRtn.routineEndTime();

                if(registeredRtnMaxEndTime < existingRtnEndTime)
                    registeredRtnMaxEndTime = existingRtnEndTime;
            }
        }

        int routineStartTime = Math.max(registeredRtnMaxEndTime, rtn.registrationTime);

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

    private void registerRelaxedStepByStep(Routine rtn, int _simulationStartTime)
    {
        int overlappintRtnMaxEndTime = _simulationStartTime;

        for(DEV_ID devId: rtn.getAllDevIDSet())
        {
            for(Routine existingRtn : lockTable.get(devId))
            {
                int existingRtnEndTime = existingRtn.routineEndTime();

                if(overlappintRtnMaxEndTime < existingRtnEndTime)
                    overlappintRtnMaxEndTime = existingRtnEndTime;
            }
        }

        int routineStartTime = Math.max(overlappintRtnMaxEndTime, rtn.registrationTime);

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

//    private void registerWeak(Routine rtn, int currentTime)
//    {
//        this.registerRoutineFromExactTime(rtn, currentTime);
//    }
//
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
                if(cmdStartTime <= scanStartTime)
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
