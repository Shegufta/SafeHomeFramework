package FailureAnalyzerSimulator;

import Temp.*;

import java.util.*;


/**
 * @author Shegufta Ahsan
 * @project SafeHomeFailureAnalyzer
 * @date 17-Sep-19
 * @time 4:17 PM
 */
public class FailureAnalyzer
{
    public Map<Float, Integer> abortHistogramGSVPSV = null;// = new HashMap<>();
    public Map<Float, Integer> rollbackHistogramGSVPSV = null;//  = new HashMap<>();
    //public Map<Float, Integer> onTheFlyHistogramGSVPSV = null;//  = new HashMap<>();

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
            if(lockTable.get(devID).isEmpty())
                continue;

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

    private Set<DEV_ID> prepareFailedDevList(double failedDevPercent, boolean atleastOneFailure, Random rand)
    {
        Set<DEV_ID> failedDevSet = new HashSet<>();

        for(DEV_ID devId : lockTableForFailureAnalysis.keySet())
        {
            float nextDbl = rand.nextFloat();
            nextDbl = (nextDbl == 1.0f) ? nextDbl - 0.001f : nextDbl;

            if(nextDbl < failedDevPercent)
            {
                failedDevSet.add(devId);
            }
        }

        if(atleastOneFailure && (failedDevSet.size() == 0) )
        {
            List<DEV_ID> tempList = new ArrayList<>();
            for(DEV_ID devId : lockTableForFailureAnalysis.keySet())
            {
                tempList.add(devId);
            }

            assert(0 < tempList.size());

            Collections.shuffle(tempList, rand);
            failedDevSet.add(tempList.get(0));
        }

        return failedDevSet;
    }

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

    public int getTotalRoutineCount()
    {
        Map<Integer, List<FailureAnalysisMetadata>> allRoutineMap = new HashMap<>();

        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            List<FailureAnalysisMetadata> lineage = lockTableForFailureAnalysis.get(devID);

            for(FailureAnalysisMetadata fam : lineage)
            {
                if(!allRoutineMap.containsKey(fam.routineID))
                    allRoutineMap.put(fam.routineID, new ArrayList<>());

                allRoutineMap.get(fam.routineID).add(fam);
            }
        }

        return allRoutineMap.size();
    }

    public Map<DEV_ID, List<FailureAnalysisMetadata>> getShadowLockTableThatContainsRoutinesExecutingBeforeOrDuringFailure(int failureTime)
    {
        Map<DEV_ID, List<FailureAnalysisMetadata>> cleanedLockTableForFailureAnalysis = new HashMap<>();

        for(DEV_ID devID : lockTableForFailureAnalysis.keySet())
        {
            if(!cleanedLockTableForFailureAnalysis.containsKey(devID))
                cleanedLockTableForFailureAnalysis.put(devID, new ArrayList<>());

            List<FailureAnalysisMetadata> lineage = lockTableForFailureAnalysis.get(devID);

            for(FailureAnalysisMetadata fam : lineage)
            {
                if(fam.rtnStartTime <= failureTime)
                {
                    cleanedLockTableForFailureAnalysis.get(devID).add(fam);
                }
            }
        }
        return cleanedLockTableForFailureAnalysis;
    }

    private Map<Integer, List<FailureAnalysisMetadata>> rebuildAllAffectedRoutineAlongWithCommands(Map<DEV_ID, List<FailureAnalysisMetadata>> cleanLockTableForFailureAnalysis, Set<DEV_ID> failedDevSet)
    {
        Map<Integer, List<FailureAnalysisMetadata>> routineIDCorrespondingFAMsortedByCommandStartTime = new HashMap<>();

        for(DEV_ID devID : cleanLockTableForFailureAnalysis.keySet())
        {
            List<FailureAnalysisMetadata> lineage = cleanLockTableForFailureAnalysis.get(devID);

            for(FailureAnalysisMetadata fam : lineage)
            {
                if(!routineIDCorrespondingFAMsortedByCommandStartTime.containsKey(fam.routineID))
                    routineIDCorrespondingFAMsortedByCommandStartTime.put(fam.routineID, new ArrayList<>());

                routineIDCorrespondingFAMsortedByCommandStartTime.get(fam.routineID).add(fam);
            }
        }


        List<Integer> unAffectedRoutineList = new ArrayList<>();
        for(int routineID : routineIDCorrespondingFAMsortedByCommandStartTime.keySet())
        {
            Collections.sort(routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID), new FailureAnalysisMetadata());

            boolean isAffected = false;

            for(int I = 0 ; I < routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).size() ; I++)
            {
                FailureAnalysisMetadata FAGSPSV = routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).get(I);
                COMMAND_STATUS cmdStatus = FAGSPSV.commandStatus;
                //DEV_ID devId = FAGSPSV.devID;

                if (consistencyType == CONSISTENCY_TYPE.STRONG || consistencyType == CONSISTENCY_TYPE.RELAXED_STRONG)
                {
                    if(cmdStatus == COMMAND_STATUS.FAILED_BEFORE_EXECUTION ||
                            cmdStatus == COMMAND_STATUS.FAILED_DURING_EXECUTION ||
                            cmdStatus == COMMAND_STATUS.FAILED_AFTER_EXECUTION)
                    {
                        isAffected = true;
                        break;
                    }
                }
                else if(consistencyType == CONSISTENCY_TYPE.EVENTUAL)
                {
                    if(isAffected)
                    {
                        routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).get(I).commandStatus = COMMAND_STATUS.IGNORE_USED_IN_EV_ONLY;
                    }
                    else if(cmdStatus == COMMAND_STATUS.FAILED_BEFORE_EXECUTION ||
                            cmdStatus == COMMAND_STATUS.FAILED_DURING_EXECUTION)
                    {
                        isAffected = true;
                        //break;
                    }
                }
                else
                {
                    System.out.println("\n ERROR: consistency type " + consistencyType + " not supported yet \nTerminating..."  );
                    System.exit(1);
                }

            }

            if(!isAffected)
                unAffectedRoutineList.add(routineID);
        }

        for(Integer unaffectedRtnID : unAffectedRoutineList)
        {
            routineIDCorrespondingFAMsortedByCommandStartTime.remove(unaffectedRtnID);
        }

        for(int routineID : routineIDCorrespondingFAMsortedByCommandStartTime.keySet())
        {
            for(int I = 1 ; I < routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).size() ; I++)
            {
                int prevEndTime = routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).get(I-1).cmdEndTime;
                int currentStartTime = routineIDCorrespondingFAMsortedByCommandStartTime.get(routineID).get(I).cmdStartTime;
                assert(prevEndTime <= currentStartTime);
            }
        }

        return routineIDCorrespondingFAMsortedByCommandStartTime;
    }

    private Set<FailureAnalysisMetadata> onTheFlySet(Map<DEV_ID, List<FailureAnalysisMetadata>> cleanLockTableForFailureAnalysis, int failureTime)
    {
        Set<FailureAnalysisMetadata> ontheFlySet = new HashSet<>();

        //int onTheFlySet = 0;

        for(DEV_ID devID : cleanLockTableForFailureAnalysis.keySet())
        {
            List<FailureAnalysisMetadata> lineage = cleanLockTableForFailureAnalysis.get(devID);

            for(FailureAnalysisMetadata fam : lineage)
            {
                //if( (fam.commandStatus != COMMAND_STATUS_GSVPSV.COMMITTED) && (fam.cmdEndTime < failureTime))
                if( (failureTime < fam.rtnEndTime) && (fam.cmdEndTime < failureTime))
                {
                    ontheFlySet.add(fam);
                    //onTheFlySet++;
                }
            }
        }
        return ontheFlySet;//onTheFlySet;
    }

    private void executeFailure(Set<DEV_ID> failedDevSet, int failureTime)
    {
        assert(consistencyType == CONSISTENCY_TYPE.STRONG || consistencyType == CONSISTENCY_TYPE.RELAXED_STRONG || consistencyType == CONSISTENCY_TYPE.EVENTUAL);


        Map<DEV_ID, List<FailureAnalysisMetadata>> cleanLockTableForFailureAnalysis; // remove the routines that starts after the failure

        cleanLockTableForFailureAnalysis = getShadowLockTableThatContainsRoutinesExecutingBeforeOrDuringFailure(failureTime);

        for(DEV_ID dev_id : cleanLockTableForFailureAnalysis.keySet())
        {
            for(FailureAnalysisMetadata fam : cleanLockTableForFailureAnalysis.get(dev_id))
            {
                fam.updateCommandStatus(failedDevSet, failureTime);
            }
        }

        //int rollbackCount = 0;
        int allRollbackCount = 0;
        //Set<FailureAnalysisMetadata> onTheFlySet = new HashSet<>();
        //int ontheFlyCount = onTheFlySet(cleanLockTableForFailureAnalysis, failureTime); // NOTE: Must call it before calling rebuildAllAffectedRoutineAlongWithCommands
        Map<Integer, List<FailureAnalysisMetadata>> affectedRoutineMap = rebuildAllAffectedRoutineAlongWithCommands(cleanLockTableForFailureAnalysis, failedDevSet);


        for(Integer abortingRoutineID : affectedRoutineMap.keySet())
        {
            int commandFailureDetectionTime = failureTime;

            if(consistencyType == CONSISTENCY_TYPE.EVENTUAL)
            {
                List<FailureAnalysisMetadata> commandList = affectedRoutineMap.get(abortingRoutineID);
                for(FailureAnalysisMetadata FAmeta : commandList)
                {
                    if(FAmeta.commandStatus == COMMAND_STATUS.FAILED_DURING_EXECUTION ||
                            FAmeta.commandStatus == COMMAND_STATUS.FAILED_BEFORE_EXECUTION)
                    {
                        commandFailureDetectionTime = FAmeta.cmdEndTime;
                        break;
                    }
                }

                //ontheFlyCount = onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime);
                //onTheFlySet.addAll(onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime));

                int totalFailureRecoveryCommandSent = 0;
                Set<DEV_ID> touchedDevSet = new HashSet<>();

                for(FailureAnalysisMetadata famd : affectedRoutineMap.get(abortingRoutineID))
                {
                    touchedDevSet.add(famd.devID);
                }

                for(DEV_ID devID : cleanLockTableForFailureAnalysis.keySet())
                {
                    if(!touchedDevSet.contains(devID))
                        continue;

                    boolean isFirstNonAbortSeen = false;
                    //boolean recoveryCommandSent = false;

                    for(int farIdx = lockTableForFailureAnalysis.get(devID).size() - 1; 0 <= farIdx ; --farIdx)
                    {
                        FailureAnalysisMetadata fam = lockTableForFailureAnalysis.get(devID).get(farIdx);
                        if( commandFailureDetectionTime < fam.cmdStartTime)
                            continue;

                        boolean isAbort = (abortingRoutineID == fam.routineID);

                        if(!isFirstNonAbortSeen && !isAbort )
                            isFirstNonAbortSeen = true;

                        //if(!recoveryCommandSent && !isFirstNonAbortSeen && isAbort)
                        if(!isFirstNonAbortSeen && isAbort)
                        {
                            totalFailureRecoveryCommandSent++;
                            break;
                        }
                    }

                }

                allRollbackCount += totalFailureRecoveryCommandSent;

//                Float data;
//
//                data = (float) rollbackCount;
//                this.rollbackHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);
//
//                data = (float) ontheFlyCount;
//                this.onTheFlyHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);
            }
            else
            {
                List<FailureAnalysisMetadata> commandList = affectedRoutineMap.get(abortingRoutineID);

                int totalCommandCount = commandList.size();
                int routineEndTime = commandList.get(0).rtnEndTime;
                //////////////////////////////////////////////////
                boolean isFailedBeforeOrDuringExecution = false;
                int successfulCommandSent = 0;
                //// first check if there is any failure before or during execution
                for(int I = 0 ; I < commandList.size() ; I++)
                {
                    FailureAnalysisMetadata fam = commandList.get(I);

                    if(fam.commandStatus == COMMAND_STATUS.FAILED_DURING_EXECUTION ||
                            fam.commandStatus == COMMAND_STATUS.FAILED_BEFORE_EXECUTION
                    )
                    {
                        commandFailureDetectionTime = fam.cmdEndTime;

                        isFailedBeforeOrDuringExecution = true;
                        break;
                    }

                    successfulCommandSent++;
                }


                if(isFailedBeforeOrDuringExecution)
                {
                    allRollbackCount += successfulCommandSent;
                    //ontheFlyCount = onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime);
                    //onTheFlySet.addAll(onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime));
                }
                else
                {
                    if(consistencyType == CONSISTENCY_TYPE.RELAXED_STRONG)
                    {
                        allRollbackCount += totalCommandCount;
                        commandFailureDetectionTime = routineEndTime;
                        //ontheFlyCount = onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime);
                        //onTheFlySet.addAll(onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime));
                        // for PSV: rollback the entire command list
                    }
                    else
                    {
                        //for GSV: rollback till the failure timestamp
                        //rollbackCount = 0;

                        int rollbackTillFailureTimestamp = 0;
                        for(int I = 0 ; I < commandList.size() ; I++)
                        {
                            FailureAnalysisMetadata fam = commandList.get(I);

                            //if(fam.cmdEndTime < failureTime)
                                //rollbackCount++;

                            if(fam.cmdEndTime < failureTime)
                                rollbackTillFailureTimestamp++;

                        }
                        //ontheFlyCount = onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime);
                        //onTheFlySet.addAll(onTheFlySet(cleanLockTableForFailureAnalysis, commandFailureDetectionTime));
                        allRollbackCount += totalCommandCount;
                    }
                }

//                Float data;
//
//                data = (float) rollbackCount;
//                this.rollbackHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);
//
//                data = (float) ontheFlyCount;
//                this.onTheFlyHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);
            }
        }

        Float data;

        data = (float) allRollbackCount;
        this.rollbackHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);

        //data = (float) onTheFlySet.size();
        //this.onTheFlyHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);


        double totalRtnCntInOriginalSubmission = getTotalRoutineCount();
        double abortCnt = affectedRoutineMap.size();

        assert(0 < totalRtnCntInOriginalSubmission);
        double abortRatio = (abortCnt / totalRtnCntInOriginalSubmission)*100.0;

        //Float data;

        data = (float) abortRatio;
        this.abortHistogramGSVPSV.merge(data, 1, (a, b) -> a + b);

    }

    Random rand;

    public void simulateFailure(double failedDevPercent, boolean atleastOneFailure,
                                int RANDOM_SEED,
                                int SampleCount,
                                ExpResults expResults)
    {
        expResults.abortHistogram = new HashMap<>();
        expResults.rollbackHistogram = new HashMap<>();
        //expResults.onTheFlyHistogram = new HashMap<>();

        this.abortHistogramGSVPSV = expResults.abortHistogram;
        this.rollbackHistogramGSVPSV = expResults.rollbackHistogram;
        //this.onTheFlyHistogramGSVPSV = expResults.onTheFlyHistogram;

        if(0 <= RANDOM_SEED)
            rand = new Random(RANDOM_SEED);
        else
            rand = new Random();

        for(int I = 0 ; I < SampleCount ; I++)
        {
            int randomFailureTime = this.minStartTime + rand.nextInt(this.maxEndTime - this.minStartTime + 1);
            Set<DEV_ID> failedDevSet = prepareFailedDevList(failedDevPercent, atleastOneFailure, rand);
            executeFailure(failedDevSet, randomFailureTime);
        }

        if(this.abortHistogramGSVPSV.isEmpty())
            this.abortHistogramGSVPSV.merge(0.0f, 0, (a, b) -> a + b);

        if(this.rollbackHistogramGSVPSV.isEmpty())
            this.rollbackHistogramGSVPSV.merge(0.0f, 0, (a, b) -> a + b);

        //if(this.onTheFlyHistogramGSVPSV.isEmpty())
            //this.onTheFlyHistogramGSVPSV.merge(0.0f, 0, (a, b) -> a + b);



        assert(!this.abortHistogramGSVPSV.isEmpty());
        assert(!this.rollbackHistogramGSVPSV.isEmpty());
        //assert(!this.onTheFlyHistogramGSVPSV.isEmpty());
    }
}
