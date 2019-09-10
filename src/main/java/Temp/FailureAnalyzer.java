package Temp;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 13-Aug-19
 * @time 3:52 PM
 */
public class FailureAnalyzer
{
    private enum COMMAND_STATUS
    {
        IGNORE,
        ON_THE_FLY,
        ABORT,
        COMMITTED,
        BEST_EFFORT
    }

    private enum ROUTINE_STATUS
    {
        COMMITTED,
        RUNNING_DURING_FAILURE,
        NOT_STARTET_YET,
        UNKNOWN
    }

    private class FailureAnalysisMetadata
    {
        public int routineID;
        public DEV_ID devID;
        public int cmdStartTime;
        public int cmdEndTime;
        public int rtnStartTime;
        public int rtnEndTime;
        public COMMAND_STATUS commandStatus;
        public ROUTINE_STATUS routineStatus;

        public boolean isMust;

        @Override
        public String toString()
        {
            String str = "";

            String isMustStr = (isMust)? "1" : "0";

            str += " {R-" + routineID +
                    ",cmd=[" + cmdStartTime + "-" + cmdEndTime + "],stat=" + commandStatus.name() +
                    "rtn=[" + rtnStartTime + "-" + rtnEndTime + "],stat=" + routineStatus.name() +
                    "isMust=" + isMustStr + "} ";
            return str;
        }

        public FailureAnalysisMetadata(final Routine _routine, DEV_ID _devId)
        {
            this.routineID = _routine.ID;
            this.devID = _devId;
            Command cmd = _routine.getCommandByDevID(this.devID);

            assert (cmd != null);
            this.isMust = cmd.isMust;
            this.cmdStartTime = cmd.startTime;
            this.cmdEndTime = cmd.getCmdEndTime();
            this.rtnStartTime = _routine.routineStartTime();
            this.rtnEndTime = _routine.routineEndTime();
            this.routineStatus = ROUTINE_STATUS.UNKNOWN;

        }

        public void updateCommandAndRoutineStatus(int failureTime)
        {
            if(this.rtnEndTime < failureTime)
            {
                routineStatus = ROUTINE_STATUS.COMMITTED;
                commandStatus = COMMAND_STATUS.COMMITTED;
            }
            else if(failureTime < this.rtnStartTime)
            {
                routineStatus = ROUTINE_STATUS.NOT_STARTET_YET;
                commandStatus = COMMAND_STATUS.IGNORE;
            }
            else
            {
                routineStatus = ROUTINE_STATUS.RUNNING_DURING_FAILURE;

                if( failureTime < cmdStartTime)
                    commandStatus = COMMAND_STATUS.IGNORE;
                else
                    commandStatus = COMMAND_STATUS.ON_THE_FLY;
            }
        }
    }

    int minStartTime = Integer.MAX_VALUE;
    int maxEndTime = Integer.MIN_VALUE;
    private Map<DEV_ID, List<FailureAnalysisMetadata>> lockTableForFailureAnalysis;
    public CONSISTENCY_TYPE consistencyType;

    public FailureAnalyzer(final Map<DEV_ID, List<Routine>> lockTable, CONSISTENCY_TYPE _consistencyType)
    {
        if(_consistencyType == CONSISTENCY_TYPE.WEAK)
            return;

        this.consistencyType = _consistencyType;
        lockTableForFailureAnalysis = new HashMap<>();
        for(DEV_ID devID : lockTable.keySet())
        {
            if(!lockTableForFailureAnalysis.containsKey(devID))
                lockTableForFailureAnalysis.put(devID, new ArrayList<>());

            for(Routine routine : lockTable.get(devID))
            {
                this.minStartTime = (routine.routineStartTime() < this.minStartTime ) ? routine.routineStartTime() : this.minStartTime;
                this.maxEndTime = (this.maxEndTime < routine.routineEndTime()) ? routine.routineEndTime() : this.maxEndTime;
                FailureAnalysisMetadata failureAnalysisMetadata = new FailureAnalysisMetadata(routine, devID);
                lockTableForFailureAnalysis.get(devID).add(failureAnalysisMetadata);
            }
        }
    }

    private List<DEV_ID> prepareFailedDevList(double failedDevPercent, boolean atleastOneFailure, Random rand)
    {
        List<DEV_ID> failedDevList = new ArrayList<>();

        for(DEV_ID devId : lockTableForFailureAnalysis.keySet())
        {
            double nextDbl = rand.nextDouble();
            nextDbl = (nextDbl == 1.0) ? nextDbl - 0.001 : nextDbl;

            if(nextDbl < failedDevPercent)
            {
                failedDevList.add(devId);
            }
        }

        if(atleastOneFailure && (failedDevList.size() == 0) )
        {
            List<DEV_ID> tempList = new ArrayList<>();
            for(DEV_ID devId : lockTableForFailureAnalysis.keySet())
            {
                tempList.add(devId);
            }

            assert(0 < tempList.size());

            Collections.shuffle(tempList, rand);
            failedDevList.add(tempList.get(0));
        }

        return failedDevList;
    }

    private void updateAllCommandAndRoutineStatus(int failureTime)
    {
        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            for(FailureAnalysisMetadata fam : lockTableForFailureAnalysis.get(devID))
            {
                fam.updateCommandAndRoutineStatus(failureTime);
            }
        }
    }

//    @Override
//    public String toString()
    public String debugString(List<DEV_ID> failedDevList, int failureTime)
    {
        String str = "failureTime = " + failureTime + "\n";
        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            if(failedDevList.contains(devID))
                str += devID.name() + "[Fail] => ";
            else
                str += devID.name() + " => ";

            for(FailureAnalysisMetadata fam : lockTableForFailureAnalysis.get(devID))
            {
                str += fam.toString();
            }
            str += "\n";
        }
        return str;
    }

    private FailureResult executeFailure(List<DEV_ID> failedDevList, int failureTime)
    {

        updateAllCommandAndRoutineStatus(failureTime);

        Set<Integer> affectedRoutineID = new HashSet<>();
        for(DEV_ID devId : failedDevList)
        {
            for(FailureAnalysisMetadata fam : lockTableForFailureAnalysis.get(devId))
            {
                if(fam.routineStatus == ROUTINE_STATUS.RUNNING_DURING_FAILURE && fam.isMust)
                    affectedRoutineID.add(fam.routineID);
            }
        }

        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            for(FailureAnalysisMetadata fam : lockTableForFailureAnalysis.get(devID))
            {
                if(affectedRoutineID.contains(fam.routineID) && fam.commandStatus == COMMAND_STATUS.ON_THE_FLY)
                {
                    if(fam.isMust)
                        fam.commandStatus = COMMAND_STATUS.ABORT;
                    else
                        fam.commandStatus = COMMAND_STATUS.BEST_EFFORT;
                }
            }
        }

        double totalOnTheFlyCmdCount = 0.0;
        double totalFailureRecoveryCommandSent = 0.0;

        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            boolean isFirstNonAbortSeen = false;
            boolean recoveryCommandSent = false;

            for(int farIdx = lockTableForFailureAnalysis.get(devID).size() - 1; 0 <= farIdx ; --farIdx)
            {
                FailureAnalysisMetadata fam = lockTableForFailureAnalysis.get(devID).get(farIdx);

                if(fam.commandStatus == COMMAND_STATUS.IGNORE)
                    continue;

                if(fam.commandStatus != COMMAND_STATUS.COMMITTED)
                    totalOnTheFlyCmdCount++;


                if(!isFirstNonAbortSeen && (fam.commandStatus != COMMAND_STATUS.ABORT) )
                    isFirstNonAbortSeen = true;

                if(!recoveryCommandSent && !isFirstNonAbortSeen && fam.commandStatus == COMMAND_STATUS.ABORT)
                    recoveryCommandSent = true;
            }

            if(recoveryCommandSent)
                totalFailureRecoveryCommandSent++;


        }

        FailureResult failureResult = new FailureResult(affectedRoutineID.size(), totalFailureRecoveryCommandSent, totalOnTheFlyCmdCount );


        return failureResult;
    }

    public FailureResult simulateFailure(double failedDevPercent, boolean atleastOneFailure)
    {
        Random rand = new Random();

        int randomFailureTime = this.minStartTime + rand.nextInt(this.maxEndTime - this.minStartTime + 1);
        List<DEV_ID> failedDevList = prepareFailedDevList(failedDevPercent, atleastOneFailure, rand);

        return executeFailure(failedDevList, randomFailureTime);

    }
}
