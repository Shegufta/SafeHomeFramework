package Temp;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 04-Sep-19
 * @time 10:37 PM
 */
public class Measurement
{
    private final int TOTAL_ISOLATION_VIOLATION_CHECK_COUNT = 3;
    /////////////////////////////////////////////
    public List<Double> parallalRtnCntList = new ArrayList<>();
    public List<Double> devUtilizationPercentList = new ArrayList<>();
    public double orderMismatchPercent = 0.0;
    //double isoltnVltnRatioAmongLineages = 0.0;
    //double isolationVltnRatioAmongRoutines = 0.0;

    public List<Double> isvltn_perRtnVictimCmdPrcntList = new ArrayList<>();
    public List<Double> isvltn_totalUniqueAttackerPerRoutineList = new ArrayList<>();
    public Double isvltn_victimRtnPercentPerRun = 0.0;
    /////////////////////////////////////////////

    private void measureDeviceUtilization(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            devUtilizationPercentList.add(100.0);
            return;
        }

        final Map<DEV_ID, List<Routine>> lockTable = _lockTable.lockTable;

        for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
        {
            int entryCount = entry.getValue().size();
            DEV_ID devID = entry.getKey();

            if(entryCount == 0)
                continue;



            double earliestAccessRequestTime = Double.MAX_VALUE;
            for(Routine rtn : entry.getValue())
            {
                if(rtn.registrationTime < earliestAccessRequestTime)
                    earliestAccessRequestTime = rtn.registrationTime;
            }



            Command lastCmd = entry.getValue().get(entryCount - 1).getCommandByDevID(devID);
            final double totalTimeSpan = lastCmd.getCmdEndTime() - earliestAccessRequestTime;

            double cmdExecutionSpan = 0.0;

            for(Routine rtn : entry.getValue())
            {
                Command cmd = rtn.getCommandByDevID(devID);
                cmdExecutionSpan += cmd.getCmdEndTime() - cmd.startTime;
            }


            double utilization = ( cmdExecutionSpan / totalTimeSpan) * 100.0;
            devUtilizationPercentList.add( utilization );
        }
    }

    private void measureParallelization(final LockTable _lockTable)
    {
        List<Routine> allRtnList = _lockTable.getAllRoutineSet(); // the special case CONSISTENCY_TYPE.WEAK is handled inside this function!

        Map<Routine, Integer> routineAndTotalOtherParallelRtnCounter_Map = new HashMap<>();

        for(Routine rtn1 : allRtnList)
        {
            routineAndTotalOtherParallelRtnCounter_Map.put(rtn1, 1); // put 1 for itself!
            int rtn1StartTimeInclusive = rtn1.routineStartTime();
            int rtn1EndTimeExclusive = rtn1.routineEndTime();

            for(Routine rtn2: allRtnList)
            {
                if(rtn1 == rtn2)
                    continue;

                boolean isParallel = rtn2.isRoutineOverlapsWithGivenTimeSpan(rtn1StartTimeInclusive, rtn1EndTimeExclusive);

                if(isParallel)
                {
                    int currentCount = routineAndTotalOtherParallelRtnCounter_Map.get(rtn1);
                    routineAndTotalOtherParallelRtnCounter_Map.put(rtn1, currentCount + 1);
                }
            }

            double parallelCountForRtn1 = routineAndTotalOtherParallelRtnCounter_Map.get(rtn1);
            this.parallalRtnCntList.add(parallelCountForRtn1);
        }

/*
        final Map<DEV_ID, List<Routine>> lockTable = _lockTable.lockTable;

        //UPDATE: I am keeping this O(n2) code!
        // DONOT REMOVE THIS CODE... MIGHT WANT TO SWITCH ON THIS VERSION LATER!
        //Actually this one and the code below are doing the same thing. This one is O(n2), where as the other one is O(n)
        for(Map.Entry<DEV_ID, List<Routine>> entry1 : lockTable.entrySet())
        {
            for(Routine rtn1 : entry1.getValue())
            {
                DEV_ID devID1 = entry1.getKey();
                Command cmd1 = rtn1.getCommandByDevID(devID1);
                int start1 = cmd1.startTime;
                double parallalCount = 0.0;

                for(Map.Entry<DEV_ID, List<Routine>> entry2 : lockTable.entrySet())
                {
                    DEV_ID devID2 = entry2.getKey();

                    if(devID1 == devID2)
                        continue;

                    for(Routine rtn2 : entry2.getValue())
                    {
                        Command cmd2 = rtn2.getCommandByDevID(devID2);

                        if( start1 < cmd2.startTime)
                            break;//avoid unnecessary search

                        if(cmd2.isCmdOverlapsWithWatchTime(start1))
                        {
                            parallalCount++;
                            break;
                        }
                    }
                }

                this.parallalRtnCntList.add(parallalCount);
            }
        }
*/

        /*
        Integer minStartTime = Integer.MAX_VALUE;
        Integer maxEndTime = Integer.MIN_VALUE;

        for(List<Routine> rtnList : lockTable.values())
        {
            for(Routine rtn : rtnList)
            {
                if(rtn.routineStartTime() < minStartTime)
                    minStartTime = rtn.routineStartTime();

                if(maxEndTime < rtn.routineEndTime())
                    maxEndTime = rtn.routineEndTime();
            }
        }

        assert(minStartTime < maxEndTime);

        int totalTimeSpan = maxEndTime - minStartTime; // start time is inclusive, end time is exclusive. e.g.  J : [<R1|C1>:1:2) [<R0|C3>:3:4) [<R2|C0>:4:5)
        int[] timeSlotArray = new int[totalTimeSpan];


        Map<Integer, Routine> routineIDRtnMap = new HashMap<>();

        for(List<Routine> rtnList : lockTable.values())
        {
            for(Routine rtn : rtnList)
            {
                if(!routineIDRtnMap.containsKey(rtn.ID))
                    routineIDRtnMap.put(rtn.ID, rtn);
            }
        }

        for(Routine rtn : routineIDRtnMap.values())
        {
            for(int I = rtn.routineStartTime() ; I < rtn.routineEndTime() ; ++I)
            {
                int index = I - minStartTime;
                timeSlotArray[index]++;
            }
        }

        for(Routine rtn : routineIDRtnMap.values())
        {
            for(Command cmd : rtn.commandList)
            {// calculate the total number of parallally running routine at each command start time
                int scanTime = cmd.startTime - minStartTime;
                double parallalFoundAtScanTime = timeSlotArray[scanTime];
                this.parallalRtnCntList.add(parallalFoundAtScanTime);
            }
        }
        */

    }

    private void measureOrderingMismatch(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            orderMismatchPercent = 0.0;
            return;
        }

        final Map<DEV_ID, List<Routine>> lockTable = _lockTable.lockTable;
        Map<Integer, Integer> routineOrderingViolation = new HashMap<>();
        double totalCount = 0.0;
        double violationCount = 0.0;

        for(List<Routine> rtnList : lockTable.values())
        {
            for(int index =0 ; index < (rtnList.size() - 1) ; index++)
            {
                totalCount++;

                if( rtnList.get(index + 1).ID < rtnList.get(index).ID)
                    violationCount++;
            }
        }

        orderMismatchPercent = (totalCount == 0.0)? 0.0 : (violationCount/totalCount)*100.0;
    }


/*
    private void isolationViolationPercentAmongLineages(final Map<DEV_ID, List<Routine>> lockTable)
    {
        Integer minStartTime = Integer.MAX_VALUE;
        Integer maxEndTime = Integer.MIN_VALUE;

        Random rand = new Random();

        for(List<Routine> rtnList : lockTable.values())
        {
            for(Routine rtn : rtnList)
            {
                if(rtn.routineStartTime() < minStartTime)
                    minStartTime = rtn.routineStartTime();

                if(maxEndTime < rtn.routineEndTime())
                    maxEndTime = rtn.routineEndTime();
            }
        }

        double sum = 0.0;

        for(int numberOfCheck = 0; numberOfCheck < TOTAL_ISOLATION_VIOLATION_CHECK_COUNT; numberOfCheck++)
        {
            double totalEvent = 0.0;
            double violationCount = 0.0;

            int randomCheckTime = minStartTime + rand.nextInt(maxEndTime - minStartTime + 1);

            for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
            {
                DEV_ID devID = entry.getKey();
                List<Routine> lineage = entry.getValue();

                if(!lineage.isEmpty())
                {// non-empty lineage
                    totalEvent++;

                    int targetDARIdx;
                    for( targetDARIdx = 0 ; targetDARIdx < lineage.size() ; ++targetDARIdx)
                    {// Search a DAR that was/is active before or at the watchTime

                        int timelineTracker = lineage.get(targetDARIdx).getCommandByDevID(devID).compareTimeline(randomCheckTime);

                        if(timelineTracker == 1)
                        { //DAR starts *after* the querytime
                            break;
                        }
                    }

                    targetDARIdx--; // point to previous DAR // Also, prevent arrayIndexOutOfBound;

                    if( 0 < targetDARIdx)
                    {
                        // if the DAR is part of a committed routine, then the system is consistent
                        // otherwise, if the DAR before that routine is not committed, then this is an inconsistant situation
                        // example of consistent lineage:   [r1c1:committed] [r2c2: ongoing] <-WatchTime
                        // example of consistent lineage:   [r1c1:committed] [r2c2: committed] <-WatchTime
                        // example of consistent lineage (only a single DAR by the WatchTime):   [r1c1:ongoing] <-WatchTime
                        // example of consistent lineage (only a single DAR by the WatchTime):   [r1c1:committed] <-WatchTime
                        // example of INCONSISTENT lineage:   [r1c1:ongoing] [r2c2: ongoing] <-WatchTime
                        if(!lineage.get(targetDARIdx).isCommittedByGivenTime(randomCheckTime))
                        {// The routine is not committed yet.

                            int prevDARidx = targetDARIdx - 1; // NOTE: 0 < targetDARIdx, hence prevDARidx is nonzero

                            if(!lineage.get(prevDARidx).isCommittedByGivenTime(randomCheckTime))
                            {//check the previousDAR. if it is ongoing, then this is an inconsistent state
                                violationCount++;
                            }
                        }
                    }


                }
            }

            double inconsistencyRatio = (totalEvent == 0.0) ? 0.0 : (violationCount/totalEvent)*100;
            sum += inconsistencyRatio;
        }

        isoltnVltnRatioAmongLineages = sum / (double) TOTAL_ISOLATION_VIOLATION_CHECK_COUNT;
    }

    private void isolationViolationPercentAmongRoutine(final Map<DEV_ID, List<Routine>> lockTable)
    {
        Map<Integer, Boolean> routineID_IsViolateMap = new HashMap<>();

        for(List<Routine> rtnList : lockTable.values())
        {
            for(Routine rtn : rtnList)
            {
                if(!routineID_IsViolateMap.containsKey(rtn.ID))
                    routineID_IsViolateMap.put(rtn.ID, false);
            }
        }

        for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
        {
            DEV_ID devID = entry.getKey();
            List<Routine> lineage = entry.getValue();

            for(int index = 1; index < lineage.size() ; index ++)
            {
                Routine currentRtn = lineage.get(index);

                if(routineID_IsViolateMap.get(currentRtn.ID))// currentRoutine has already violated the isolation. don't need to check it again.
                    continue;

                Command currentCmd = currentRtn.getCommandByDevID(devID);
                int crntCmdStartTime = currentCmd.startTime; // this command is running
                // so it is ensured that currentRtn has not been committed yet.
                // check if the previous rtn has been committed or not

                Routine prevRtn = lineage.get(index - 1);

                if(!prevRtn.isCommittedByGivenTime(crntCmdStartTime))
                {//currentRtn starts a command before prevRtn committed, hence currentRtn violates the isolation
                    routineID_IsViolateMap.put(currentRtn.ID, true);
                }
            }
        }

        double totalRtnCount = routineID_IsViolateMap.size();
        double violationCount = 0.0;

        for(boolean isViolated : routineID_IsViolateMap.values())
        {
            if(isViolated)
                violationCount++;
        }

        double violationPercentage = (totalRtnCount == 0.0)? 0.0 : ((violationCount/totalRtnCount)*100.0);
        isolationVltnRatioAmongRoutines = violationPercentage;
    }
*/
    private void isolationViolation(final LockTable _lockTable)
    {
        List<Routine> allRtnList = _lockTable.getAllRoutineSet();

        Map<Routine, Set<Routine>> victimRtnAndAttackerRtnSetMap = new HashMap<>();
        Map<Routine, Set<Command>> victimRtnAndItsVictimCmdSetMap = new HashMap<>();

        for(Routine rtn1 : allRtnList)
        {
            assert(!victimRtnAndAttackerRtnSetMap.containsKey(rtn1));
            victimRtnAndAttackerRtnSetMap.put(rtn1, new HashSet<>());

            assert(!victimRtnAndItsVictimCmdSetMap.containsKey(rtn1));
            victimRtnAndItsVictimCmdSetMap.put(rtn1, new HashSet<>());

            for(Routine rtn2 : allRtnList)
            {
                if(rtn1 == rtn2)
                    continue;

                for(Command cmd1 : rtn1.commandList)
                {
                    DEV_ID devID = cmd1.devID;
                    int spanStartTimeInclusive = cmd1.startTime;
                    int spanEndTimeExclusive = rtn1.routineEndTime();

                    boolean isAttackedByRtn2 = rtn2.isDevAccessStartsDuringTimeSpan(devID, spanStartTimeInclusive, spanEndTimeExclusive);

                    if(isAttackedByRtn2)
                    {
                        victimRtnAndAttackerRtnSetMap.get(rtn1).add(rtn2);
                        victimRtnAndItsVictimCmdSetMap.get(rtn1).add(cmd1);
                    }
                }
            }
        }

        double victimRoutineCount = 0.0;

        for(Routine rtn1 : allRtnList)
        {
            double victimCommandCount = victimRtnAndItsVictimCmdSetMap.get(rtn1).size();

            if(victimCommandCount == 0.0)
            {// no violation for this routine
                isvltn_perRtnVictimCmdPrcntList.add(0.0);
                isvltn_totalUniqueAttackerPerRoutineList.add(0.0);
            }
            else
            {
                victimRoutineCount++;
                double totalCommand = rtn1.commandList.size();

                double perRtnSpoiledCmdPercent = (victimCommandCount/totalCommand)*100.0;
                isvltn_perRtnVictimCmdPrcntList.add(perRtnSpoiledCmdPercent);
                isvltn_totalUniqueAttackerPerRoutineList.add((double)victimRtnAndAttackerRtnSetMap.get(rtn1).size());
            }
        }

        double totalRoutine = allRtnList.size();
        isvltn_victimRtnPercentPerRun = (victimRoutineCount / totalRoutine)*100.0;

    }

    public Measurement(final LockTable lockTable, CONSISTENCY_TYPE _consistencyType)
    {
        measureParallelization(lockTable);
        measureOrderingMismatch(lockTable);
        //isolationViolationPercentAmongLineages(lockTable);
        measureDeviceUtilization(lockTable);
        //isolationViolationPercentAmongRoutine(lockTable);
        isolationViolation(lockTable);
    }
}
