/**
 * Measurement for SafeHome
 *
 * Measurement stores the metric data for further data analytics. It is used
 * to measure and store the metric that are measured based on LockTable.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 04-Sep-19
 * @time 10:37 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package SafeHomeSimulator;
import java.util.*;


public class Measurement
{
    public Map<Float, Integer> deltaParallelismHistogram = new HashMap<>();
    public Map<Float, Integer> rawParallelismHistogram = new HashMap<>();
    //Map<Float, Integer> orderingMismatchPrcntHistogram = new HashMap<>();
    public Map<Float, Integer> orderingMismatchPrcntBUBBLEHistogram = new HashMap<>();
    public Map<Float, Integer> devUtilizationPrcntHistogram = new HashMap<>();

    //public List<Float> devUtilizationPercentList = new ArrayList<>();
    //public float orderMismatchPercent = 0.0f;

    public Map<Float, Integer> isvltn1_perRtnCollisionCountHistogram = new HashMap<>();
    public Map<Float, Integer> isvltn3_CMDviolationPercentHistogram = new HashMap<>(); // Command Violation Per Routine
    public Map<Float, Integer> isvltn2_RTNviolationPercentHistogram = new HashMap<>();
    public Map<Float, Integer> isvltn4_cmdToCommitCollisionTimespanPrcntHistogram = new HashMap<>();
    public Map<Float, Integer> isvltn5_routineLvlIsolationViolationTimePrcntHistogram = new HashMap<>();
    //ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT
    /////////////////////////////////////////////

    private class RtnSpan
    {
        int startInclusive;
        int endExclusive;
        public RtnSpan(int _startInclusive, int _endExclusive)
        {
            this.startInclusive = _startInclusive;
            this.endExclusive = _endExclusive;
        }

        @Override
        public String toString()
        {
            return "[" + this.startInclusive + ", " + this.endExclusive + ")";
        }
    }

    private void measureDeviceUtilization(final LockTable _lockTable)
    {
//        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
//        {
//            this.devUtilizationPrcntHistogram.put(100.0f, 1);
//            return;
//        }
//        else
//        {
        Map<DEV_ID, List<Routine>> lockTable;

        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            lockTable = _lockTable.perDevRoutineListForWeakScheduling;
        }
        else
        {
            lockTable = _lockTable.lockTable;
        }

        for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
        {
            DEV_ID devID = entry.getKey();
            List<Routine> lineage = entry.getValue();
            int entryCount = lineage.size();

            if(entryCount == 0)
                continue;

            float earliestAccessRequestTime = Float.MAX_VALUE;
            float lastAccessTime = Float.MIN_VALUE;
            for(Routine rtn : lineage)
            {
                if(rtn.registrationTime < earliestAccessRequestTime)
                    earliestAccessRequestTime = rtn.registrationTime;

                if(lastAccessTime < rtn.getCommandByDevID(devID).getCmdEndTime())
                    lastAccessTime = rtn.getCommandByDevID(devID).getCmdEndTime();
            }

            //Command lastCmd = lineage.get(entryCount - 1).getCommandByDevID(devID);
            //final double totalTimeSpan = lastCmd.getCmdEndTime() - earliestAccessRequestTime;
            final double totalTimeSpan = lastAccessTime - earliestAccessRequestTime;

            double cmdExecutionSpan = 0.0f;

            if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
            {// count overlapping device usages as a single use
                //ONLY WV will have overlapping device access
                Set<Integer> uniqueTimeTrackerForWV = new HashSet<>();

                for(Routine rtn : lineage)
                {
                    Command cmd = rtn.getCommandByDevID(devID);

                    for(int start = cmd.startTime ; start < cmd.getCmdEndTime() ; start++)
                    {
                        uniqueTimeTrackerForWV.add(start);
                    }
                }

                cmdExecutionSpan = uniqueTimeTrackerForWV.size();
            }
            else
            {
                for(Routine rtn : lineage)
                {
                    Command cmd = rtn.getCommandByDevID(devID);
                    cmdExecutionSpan += cmd.getCmdEndTime() - cmd.startTime;
                }
            }



            double utilization = ( cmdExecutionSpan / totalTimeSpan) * 100.0;
            //devUtilizationPercentList.add( utilization );



            Float data = (float)utilization;
            Integer count = this.devUtilizationPrcntHistogram.get(data);

            if(count == null)
                this.devUtilizationPrcntHistogram.put(data, 1);
            else
                this.devUtilizationPrcntHistogram.put(data, count + 1);
        }
        //}
    }

    private void measureParallelization(final LockTable _lockTable)
    {
        //System.out.printf("Start measuring Parallel delta for consistency type %s\n", _lockTable.consistencyType.name());
        Integer minStartTimeInclusive = Integer.MAX_VALUE;
        Integer maxEndTimeExclusive = Integer.MIN_VALUE;

        for(Routine rtn : _lockTable.getAllRoutineSet())
        {
            if(rtn.routineStartTime() < minStartTimeInclusive)
                minStartTimeInclusive = rtn.routineStartTime();

            if(maxEndTimeExclusive < rtn.routineEndTime())
                maxEndTimeExclusive = rtn.routineEndTime();
        }

        assert(minStartTimeInclusive < maxEndTimeExclusive);

        int totalTimeSpan = maxEndTimeExclusive - minStartTimeInclusive; // start time is inclusive, end time is exclusive. e.g.  J : [<R1|C1>:1:2) [<R0|C3>:3:4) [<R2|C0>:4:5)

        //System.out.printf("Total time span: %d with start %d end %d \n", totalTimeSpan, minStartTimeInclusive, maxEndTimeExclusive);

        //this.parallelRtnCntList = new ArrayList<>(Collections.nCopies(totalTimeSpan, 0.0f));

        short[] histogram = new short[totalTimeSpan];

        for(Routine rtn : _lockTable.getAllRoutineSet())
        {
            int startIdx = rtn.routineStartTime() - minStartTimeInclusive;
            int endIdx = rtn.routineEndTime() - minStartTimeInclusive;

            //System.out.printf("rtn %s start idx: %d, end idx: %d\n", rtn.ID, startIdx, endIdx);

            for(int I = startIdx ; I < endIdx ; I++)
            {
                histogram[I]++;
                //this.parallelRtnCntList.add(I, (this.parallelRtnCntList.get(I) + 1));
            }
        }


        for(float frequency : histogram)
        {
            Integer count = rawParallelismHistogram.get(frequency);
            // here the count is the data. we have to count how many time these "count" appear

            if(count == null)
                rawParallelismHistogram.put(frequency, 1);
            else
                rawParallelismHistogram.put(frequency, count + 1);
        }


        short currentFreq = -1;

        for(short frequency : histogram)
        {
            // New Approach: just record the change in frequency...
            // e.g.  if the freq is 1 1 1 1 3 3 2 1 => then record 1,3,2,1...
            // i.e. just the changing points
            if(frequency != currentFreq)
            {
                //System.out.printf("A different parallel level %d\n", frequency);
                currentFreq = frequency;

                Integer count = deltaParallelismHistogram.get((float)frequency);
                // here the count is the data. we have to count how many time these "count" appear

                if(count == null)
                    deltaParallelismHistogram.put((float)frequency, 1);
                else
                    deltaParallelismHistogram.put((float)frequency, count + 1);
            }
        }
        // here the count is the data. we have to count how many time these "count" appear

        deltaParallelismHistogram.merge((float) 0, 1, Integer::sum);
        //System.out.printf("Size of parallelism data: %d\n", deltaParallelismHistogram.size());
    }

    /*
    private void measureOrderingMismatch(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            this.orderingMismatchPrcntHistogram.put(-1.0f, -1);
        }
        else
        {
            for(List<Routine> lineage : _lockTable.lockTable.values())
            {
                double orderMismatchPercent = 0.0;

                if(lineage.size() == 1)
                {
                    orderMismatchPercent = 0.0;
                }
                else
                {
                    Map<Integer, Integer> rtnIDVsCurrentIndexMap = new HashMap<>();
                    List<Integer> sortedRtnList = new ArrayList<>();

                    int index = 0;
                    for(Routine rtn : lineage)
                    {
                        rtnIDVsCurrentIndexMap.put(rtn.ID, index++);
                        sortedRtnList.add(rtn.ID);
                    }

                    Collections.sort(sortedRtnList);

                    double maxPossibleMismatch = 0;
                    double orderMismatch = 0;
                    final int maxIndex = sortedRtnList.size() - 1;

                    for(int I = 0 ; I < sortedRtnList.size() ; I++)
                    {
                        int rtnID = sortedRtnList.get(I);
                        int indexInLineage = rtnIDVsCurrentIndexMap.get(rtnID);

                        orderMismatch += Math.abs(I - indexInLineage);
                        maxPossibleMismatch += Math.abs(2*I - maxIndex);
                    }

                    orderMismatchPercent = (orderMismatch / maxPossibleMismatch) * 100.0;
                }

                Float data = (float)orderMismatchPercent;
                Integer count = this.orderingMismatchPrcntHistogram.get(data);

                if(count == null)
                    this.orderingMismatchPrcntHistogram.put(data, 1);
                else
                    this.orderingMismatchPrcntHistogram.put(data, count + 1);
            }
        }
    }
*/

    private void measureOrderingMismatchBubble(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            this.orderingMismatchPrcntBUBBLEHistogram.put(-1.0f, -1);
        }
        else
        {
            for (List<Routine> lineage : _lockTable.lockTable.values())
            {
                double orderMismatchPercent = 0.0;
                if (lineage.size() <= 1)
                {
                    orderMismatchPercent = 0.0;
                }
                else
                {
                    List<Integer> rtnList = new ArrayList<>();
                    for (Routine rtn : lineage)
                    {
                        rtnList.add(rtn.ID);
                    }
                    int num_swap = sortRtnList(rtnList);
                    int max_swap = rtnList.size() * (rtnList.size() - 1) / 2;
                    orderMismatchPercent = (num_swap * 100.0 / max_swap);
                }

                Float data = (float) orderMismatchPercent;
                this.orderingMismatchPrcntBUBBLEHistogram.merge(data, 1, (a, b) -> a + b);
            }
        }
    }

    private int sortRtnList(List<Integer> rtnList)
    {
        int i, j, temp;
        int swaps = 0;
        for(i = 0; i < rtnList.size() - 1; ++i)
        {
            for(j=0; j< rtnList.size() - 1 - i; ++j)
            {

                if(rtnList.get(j) > rtnList.get(j+1))
                {

                    temp = rtnList.get(j+1);
                    rtnList.set(j+1, rtnList.get(j));
                    rtnList.set(j, temp);
                    swaps++;
                }
            }
        }
        return swaps;
    }



    private void isolationViolation(final LockTable _lockTable)
    {
        List<Routine> allRtnList = _lockTable.getAllRoutineSet();

        Map<Routine, Set<Routine>> victimRtnAndAttackerRtnSetMap = new HashMap<>();
        Map<Routine, Set<Command>> victimRtnAndItsVictimCmdSetMap = new HashMap<>();
        Map<Routine, Float> victimRtnAndEarliestCollisionTimeMap = new HashMap<>();

        for(Routine rtn1 : allRtnList)
        {
            assert(!victimRtnAndAttackerRtnSetMap.containsKey(rtn1));
            victimRtnAndAttackerRtnSetMap.put(rtn1, new HashSet<>());

            assert(!victimRtnAndItsVictimCmdSetMap.containsKey(rtn1));
            victimRtnAndItsVictimCmdSetMap.put(rtn1, new HashSet<>());


            for(Command cmd1 : rtn1.commandList)
            {
                DEV_ID devID = cmd1.devID;
                int spanStartTimeInclusive = cmd1.startTime;
                int spanEndTimeExclusive = rtn1.routineEndTime();

                float earliestCollisionTime = Float.MAX_VALUE;

                for (Routine rtn2 : allRtnList)
                {
                    if (rtn1 == rtn2)
                        continue;

                    boolean isAttackedByRtn2 = rtn2.isDevAccessStartsDuringTimeSpan(devID, spanStartTimeInclusive, spanEndTimeExclusive);

                    if (isAttackedByRtn2)
                    {
                        victimRtnAndAttackerRtnSetMap.get(rtn1).add(rtn2);
                        victimRtnAndItsVictimCmdSetMap.get(rtn1).add(cmd1);

                        float collisionTime = rtn2.getCommandByDevID(devID).startTime;

                        if(collisionTime < earliestCollisionTime)
                            earliestCollisionTime = collisionTime;

                        /*
                        System.out.printf("Incongrance with rtn ID: %d\n" +
                                "    conflicting rtn ID: %d\n" +
                                "    conflicting cmd devID: %s",
                            rtn1.ID, rtn2.ID, cmd1.devID);

                         */

                    }
                }

                float timeSpentInCollisionRatio = 0.0f;

                if(earliestCollisionTime < Float.MAX_VALUE)
                {
                    assert(spanStartTimeInclusive <= earliestCollisionTime);

                    float expectedConsistencySpanCmd1 = spanEndTimeExclusive - spanStartTimeInclusive;
                    float collisionTime = spanEndTimeExclusive - earliestCollisionTime;

                    timeSpentInCollisionRatio = (collisionTime / expectedConsistencySpanCmd1) * 100.0f;


                    if(!victimRtnAndEarliestCollisionTimeMap.containsKey(rtn1))
                    {// to analysis routine-timespan-level collision, we need only the first colliding command
                        // hence this check is required. if we have already seen a command, there is no need to check for next violation
                        // as in routine level, isolation-violation starts from the very first command-violation;
                        victimRtnAndEarliestCollisionTimeMap.put(rtn1, earliestCollisionTime);
                    }
                }

                Float data = timeSpentInCollisionRatio;
                Integer count = this.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram.get(data);

                if(count == null)
                    this.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram.put(data, 1);
                else
                    this.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram.put(data, count + 1);
            }

        }


        float victimRoutineCount = 0.0f;

        for(Routine rtn1 : allRtnList)
        {
            float victimCommandCount = victimRtnAndItsVictimCmdSetMap.get(rtn1).size();

            float isvltn_perRtnVictimCmdPrcnt = 0.0f;
            float isvltn_totalUniqueAttackerPerRoutine = 0.0f;

            if(0.0 < victimCommandCount)
            {
                victimRoutineCount++;
                float totalCommand = rtn1.commandList.size();

                isvltn_perRtnVictimCmdPrcnt = (victimCommandCount/totalCommand)*100.0f;
                isvltn_totalUniqueAttackerPerRoutine = (float)victimRtnAndAttackerRtnSetMap.get(rtn1).size();
            }

            //////////////////////////////////////////////////////////////////
            Float data;
            Integer count;
            //////////////////////////////////////////
            //isvltn_perRtnVictimCmdPrcntList.add(perRtnSpoiledCmdPercent);
            data = isvltn_perRtnVictimCmdPrcnt;
            count = this.isvltn3_CMDviolationPercentHistogram.get(data);

            if(count == null)
                this.isvltn3_CMDviolationPercentHistogram.put(data, 1);
            else
                this.isvltn3_CMDviolationPercentHistogram.put(data, count + 1);
            //////////////////////////////////////////////////////////////////

            data = isvltn_totalUniqueAttackerPerRoutine;
            count = this.isvltn1_perRtnCollisionCountHistogram.get(data);

            if(count == null)
                this.isvltn1_perRtnCollisionCountHistogram.put(data, 1);
            else
                this.isvltn1_perRtnCollisionCountHistogram.put(data, count + 1);
            //////////////////////////////////////////////////////////////////
        }
        /////////////////////////////////////////////////////////////////
        float totalRoutine = allRtnList.size();
        //isvltn_victimRtnPercentPerRun = (victimRoutineCount / totalRoutine)*100.0f;
        Float data = (victimRoutineCount / totalRoutine)*100.0f;
        Integer count = this.isvltn2_RTNviolationPercentHistogram.get(data);

        if(count == null)
            this.isvltn2_RTNviolationPercentHistogram.put(data, 1);
        else
            this.isvltn2_RTNviolationPercentHistogram.put(data, count + 1);
        /////////////////////////////////////////////////////////////////


        for(Routine rtn1 : allRtnList)
        {
            float timeSpentInCollisionRatio = 0.0f;

            if(victimRtnAndEarliestCollisionTimeMap.containsKey(rtn1))
            {// this routine has violation!
                float earliestCollisionTime = victimRtnAndEarliestCollisionTimeMap.get(rtn1);
                int spanStartTimeInclusive = rtn1.routineStartTime();
                int spanEndTimeExclusive = rtn1.routineEndTime();
                float expectedConsistencySpanRtn1 = spanEndTimeExclusive - spanStartTimeInclusive;
                float collisionTime = spanEndTimeExclusive - earliestCollisionTime;

//                if (collisionTime > 0) {
//                    System.out.printf("Incongrance with rtn ID: %d\n" +
//                            "    Start time %d\n" +
//                            "    End time %d\n" +
//                            "    Earlist collision time %f\n" +
//                            "    collisionTime %f, spanTime %f\n",
//                        rtn1.ID, spanStartTimeInclusive, spanEndTimeExclusive, earliestCollisionTime,
//                        collisionTime, expectedConsistencySpanRtn1);
//                }

                timeSpentInCollisionRatio = (collisionTime / expectedConsistencySpanRtn1) * 100.0f;
            }

            data = timeSpentInCollisionRatio;
            this.isvltn5_routineLvlIsolationViolationTimePrcntHistogram.merge(data, 1, (a, b) -> a + b);
        }
    }

    public Map<DEV_ID, Routine> devID_lastAccesedRtn_Map;

    public Measurement(final LockTable lockTable)
    {
        devID_lastAccesedRtn_Map = lockTable.devID_lastAccesedRtn_Map;

        measureParallelization(lockTable);
        //measureOrderingMismatch(lockTable);
        measureOrderingMismatchBubble(lockTable);
        measureDeviceUtilization(lockTable);
        isolationViolation(lockTable);


        assert(!isvltn3_CMDviolationPercentHistogram.isEmpty());
        assert(!isvltn1_perRtnCollisionCountHistogram.isEmpty());
        assert(!isvltn2_RTNviolationPercentHistogram.isEmpty());
        assert(!isvltn4_cmdToCommitCollisionTimespanPrcntHistogram.isEmpty());
        assert(!isvltn5_routineLvlIsolationViolationTimePrcntHistogram.isEmpty());
        //assert(!orderingMismatchPrcntHistogram.isEmpty());
        assert(!orderingMismatchPrcntBUBBLEHistogram.isEmpty());
        assert(!deltaParallelismHistogram.isEmpty());
        assert(!rawParallelismHistogram.isEmpty());
        assert(!devUtilizationPrcntHistogram.isEmpty());
    }
}
