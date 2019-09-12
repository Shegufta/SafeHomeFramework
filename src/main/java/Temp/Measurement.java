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
    Map<Float, Float> parallelismHistogram = new HashMap<>();

    public List<Float> devUtilizationPercentList = new ArrayList<>();
    public float orderMismatchPercent = 0.0f;

    //public List<Float> isvltn_perRtnVictimCmdPrcntList = new ArrayList<>();
    Map<Float, Float> isvltn_perRtnVictimCmdPrcntHistogram = new HashMap<>();

    //public List<Float> isvltn_totalUniqueAttackerPerRoutineList = new ArrayList<>();
    Map<Float, Float> isvltn_totalUniqueAttackerPerRoutineHistogram = new HashMap<>();

    //public float isvltn_victimRtnPercentPerRun = 0.0f;
    Map<Float, Float> isvltn_victimRtnPercentHistogram = new HashMap<>();
    /////////////////////////////////////////////

    private void measureDeviceUtilization(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            devUtilizationPercentList.add(100.0f);
            return;
        }

        final Map<DEV_ID, List<Routine>> lockTable = _lockTable.lockTable;

        for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
        {
            int entryCount = entry.getValue().size();
            DEV_ID devID = entry.getKey();

            if(entryCount == 0)
                continue;

            float earliestAccessRequestTime = Float.MAX_VALUE;
            for(Routine rtn : entry.getValue())
            {
                if(rtn.registrationTime < earliestAccessRequestTime)
                    earliestAccessRequestTime = rtn.registrationTime;
            }

            Command lastCmd = entry.getValue().get(entryCount - 1).getCommandByDevID(devID);
            final float totalTimeSpan = lastCmd.getCmdEndTime() - earliestAccessRequestTime;

            float cmdExecutionSpan = 0.0f;

            for(Routine rtn : entry.getValue())
            {
                Command cmd = rtn.getCommandByDevID(devID);
                cmdExecutionSpan += cmd.getCmdEndTime() - cmd.startTime;
            }


            float utilization = ( cmdExecutionSpan / totalTimeSpan) * 100.0f;
            devUtilizationPercentList.add( utilization );
        }
    }

    private void measureParallelization(final LockTable _lockTable)
    {
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



        //this.parallelRtnCntList = new ArrayList<>(Collections.nCopies(totalTimeSpan, 0.0f));

        int[] histogram = new int[totalTimeSpan];

        for(Routine rtn : _lockTable.getAllRoutineSet())
        {
            int startIdx = rtn.routineStartTime() - minStartTimeInclusive;
            int endIdx = rtn.routineEndTime() - minStartTimeInclusive;

            for(int I = startIdx ; I < endIdx ; I++)
            {
                histogram[I]++;
                //this.parallelRtnCntList.add(I, (this.parallelRtnCntList.get(I) + 1));
            }
        }

        for(float frequency : histogram)
        {
            Float count = parallelismHistogram.get(frequency);
            // here the count is the data. we have to count how many time these "count" appear

            if(count == null)
                parallelismHistogram.put(frequency, 1f);
            else
                parallelismHistogram.put(frequency, count + 1f);
        }

        assert(!parallelismHistogram.isEmpty());
    }

    private void measureOrderingMismatch(final LockTable _lockTable)
    {
        if(_lockTable.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            orderMismatchPercent = -1.0f;
            return;
        }

        final Map<DEV_ID, List<Routine>> lockTable = _lockTable.lockTable;
        Map<Integer, Integer> routineOrderingViolation = new HashMap<>();
        float totalCount = 0.0f;
        float violationCount = 0.0f;

        for(List<Routine> rtnList : lockTable.values())
        {
            for(int index =0 ; index < (rtnList.size() - 1) ; index++)
            {
                totalCount++;

                if( rtnList.get(index + 1).ID < rtnList.get(index).ID)
                    violationCount++;
            }
        }

        orderMismatchPercent = (totalCount == 0.0f)? 0.0f : (violationCount/totalCount)*100.0f;
    }

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
            Float count;
            //////////////////////////////////////////
            //isvltn_perRtnVictimCmdPrcntList.add(perRtnSpoiledCmdPercent);
            data = isvltn_perRtnVictimCmdPrcnt;
            count = this.isvltn_perRtnVictimCmdPrcntHistogram.get(data);

            if(count == null)
                this.isvltn_perRtnVictimCmdPrcntHistogram.put(data, 1f);
            else
                this.isvltn_perRtnVictimCmdPrcntHistogram.put(data, count + 1f);
            //////////////////////////////////////////////////////////////////

            data = isvltn_totalUniqueAttackerPerRoutine;
            count = this.isvltn_totalUniqueAttackerPerRoutineHistogram.get(data);

            if(count == null)
                this.isvltn_totalUniqueAttackerPerRoutineHistogram.put(data, 1f);
            else
                this.isvltn_totalUniqueAttackerPerRoutineHistogram.put(data, count + 1f);
            //////////////////////////////////////////////////////////////////
        }


        /////////////////////////////////////////////////////////////////
        float totalRoutine = allRtnList.size();
        //isvltn_victimRtnPercentPerRun = (victimRoutineCount / totalRoutine)*100.0f;
        Float data = (victimRoutineCount / totalRoutine)*100.0f;
        Float count = this.isvltn_victimRtnPercentHistogram.get(data);

        if(count == null)
            this.isvltn_victimRtnPercentHistogram.put(data, 1f);
        else
            this.isvltn_victimRtnPercentHistogram.put(data, count + 1f);
        /////////////////////////////////////////////////////////////////

        assert(!isvltn_perRtnVictimCmdPrcntHistogram.isEmpty());
        assert(!isvltn_totalUniqueAttackerPerRoutineHistogram.isEmpty());
        assert(!isvltn_victimRtnPercentHistogram.isEmpty());
    }

    public Measurement(final LockTable lockTable)
    {
        measureParallelization(lockTable);
        measureOrderingMismatch(lockTable);
        measureDeviceUtilization(lockTable);
        isolationViolation(lockTable);
    }
}
