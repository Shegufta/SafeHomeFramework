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
    private final int TOTAL_INCONSISTENCY_CHECK_COUNTER = 3;
    /////////////////////////////////////////////
    List<Double> parallalIncidentCountList = new ArrayList<>();
    List<Double> devUtilizationList = new ArrayList<>();
    //double maxParallalRtnCnt = Integer.MIN_VALUE;
    //double avgParallalRtnCnt;
    double orderMismatchPercent = 0.0;
    double avgInconsistencyRatio = 0.0;
    /////////////////////////////////////////////

    private void measureDeviceUtilization(final Map<DEV_ID, List<Routine>> lockTable)
    {
        /*
        double totalGap = 0.0;

        for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
        {
            DEV_ID devID = entry.getKey();

            for(int index =0 ; index < (entry.getValue().size() - 1) ; index++)
            {
                Command cmd1 = entry.getValue().get(index).getCommandByDevID(devID);
                Command cmd2 = entry.getValue().get(index + 1).getCommandByDevID(devID);

                totalGap += cmd2.startTime - cmd1.getCmdEndTime();
            }
        }

        devUtilizationList.add(totalGap);
        */

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
            devUtilizationList.add( utilization );


            /*
            int entryCount = entry.getValue().size();
            DEV_ID devID = entry.getKey();

            if(entryCount == 0)
                continue;

            if(entryCount == 1)
            {
                devUtilizationList.add(100.0);
            }
            else
            {
                Command firstCmd = entry.getValue().get(0).getCommandByDevID(devID);
                Command lastCmd = entry.getValue().get(entryCount - 1).getCommandByDevID(devID);

                final double totalTimeSpan = lastCmd.getCmdEndTime() - firstCmd.startTime;
                double cmdExecutionSpan = 0.0;

                for(Routine rtn : entry.getValue())
                {
                    Command cmd = rtn.getCommandByDevID(devID);
                    cmdExecutionSpan += cmd.getCmdEndTime() - cmd.startTime;
                }
                devUtilizationList.add( ( cmdExecutionSpan / totalTimeSpan) * 100.0 );
            }
            */
        }

    }

    private void measureParallelization(final Map<DEV_ID, List<Routine>> lockTable)
    {
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

                this.parallalIncidentCountList.add(parallalCount);
            }
        }




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

        double sum = 0.0;

        for(int index = 0 ; index < timeSlotArray.length ; ++index)
        {
            if( maxParallalRtnCnt < timeSlotArray[index])
                maxParallalRtnCnt = timeSlotArray[index];

            sum += timeSlotArray[index];
        }

        avgParallalRtnCnt = sum / (double)totalTimeSpan;

        */
    }

    private void measureOrderingMismatch(final Map<DEV_ID, List<Routine>> lockTable)
    {
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

    private void inconsistencyMeasurement(final Map<DEV_ID, List<Routine>> lockTable)
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

        for(int numberOfCheck = 0 ; numberOfCheck < TOTAL_INCONSISTENCY_CHECK_COUNTER ; numberOfCheck++)
        {
            double totalEvent = 0.0;
            double inconsistencyCount = 0.0;

            int randomWatchTime = minStartTime + rand.nextInt(maxEndTime - minStartTime + 1);
            for(Map.Entry<DEV_ID, List<Routine>> entry : lockTable.entrySet())
            {
                DEV_ID devID = entry.getKey();
                List<Routine> rtnList = entry.getValue();

                if(!rtnList.isEmpty())
                {// non-empty lineage
                    totalEvent++;

                    int targetDARIdx;
                    for( targetDARIdx = 0 ; targetDARIdx < rtnList.size() ; ++targetDARIdx)
                    {// Search a DAR that was/is active before or at the watchTime

                        int timelineTracker = rtnList.get(targetDARIdx).getCommandByDevID(devID).compareTimeline(randomWatchTime);

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
                        if(!rtnList.get(targetDARIdx).isCommittedByGivenTime(randomWatchTime))
                        {// The routine is not committed yet.

                            int prevDARidx = targetDARIdx - 1; // NOTE: 0 < targetDARIdx, hence prevDARidx is nonzero

                            if(!rtnList.get(prevDARidx).isCommittedByGivenTime(randomWatchTime))
                            {//check the previousDAR. if it is ongoing, then this is an inconsistent state
                                inconsistencyCount++;
                            }
                        }
                    }


                }
            }

            double inconsistencyRatio = (totalEvent == 0.0) ? 0.0 : (inconsistencyCount/totalEvent)*100;
            sum += inconsistencyRatio;
        }

        avgInconsistencyRatio = sum / (double)TOTAL_INCONSISTENCY_CHECK_COUNTER;
    }


    public Measurement(final Map<DEV_ID, List<Routine>> lockTable)
    {
        measureParallelization(lockTable);
        measureOrderingMismatch(lockTable);
        inconsistencyMeasurement(lockTable);
        measureDeviceUtilization(lockTable);
    }
}
