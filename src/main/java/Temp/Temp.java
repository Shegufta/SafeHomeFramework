package Temp;


import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 17-Jul-19
 * @time 10:32 AM
 */
public class Temp
{
    public static final int MAX_DATAPOINT_COLLECTON_SIZE = 3000;

    public static final boolean IS_PRE_LEASE_ALLOWED = true;
    public static final boolean IS_POST_LEASE_ALLOWED = true;

    private static final double timelineMultiplierForSporadicRoutines = 0.5; // works only if "isSentAllRtnSameTime == false"; the timeline will vary upto N times
    private static final boolean isSentAllRtnSameTime = false;
    private static int maxConcurrentRtn = 5; //in current version totalConcurrentRtn = maxConcurrentRtn;

    private static int maxCommandPerRtn = 6; // in current version totalCommandInThisRtn = maxCommandPerRtn;

    private static double zipfCoefficient = 0.1;
    private static double longRunningRtnPercentage = .05;
    private static final boolean atleastOneLongRunning = false;

    private static double devFailureRatio = 0.0;
    private static final boolean atleastOneDevFail = false;
    private static double mustCmdPercentage = 1.0;

    private static int weakVisibilityDeviceAccessTime = 1;

    private static int longRunningCmdDuration = 2000;
    private static final boolean isLongCmdDurationVary = true;
    private static final double longCmdDurationVaryMultiplier = 2.0; // will vary upto N times

    private static final int shortCmdDuration = 5;
    private static final boolean isShortCmdDurationVary = true;
    private static final double shortCmdDurationVaryMultiplier = 3; // will vary upto N times

    private static final int totalSampleCount = 10000; // 100000;
    private static final boolean isPrint = false;

    private static List<DEV_ID> devIDlist = new ArrayList<>();
    private static Map<DEV_ID, ZipfProbBoundary> devID_ProbBoundaryMap = new HashMap<>();

    private static final int SIMULATION_START_TIME = 0;


    private static DEV_ID getZipfDistDevID(double randDouble)
    {
        assert(0 < devID_ProbBoundaryMap.size());
        assert(0.0 <= randDouble && randDouble <= 1.0);

        for(Map.Entry<DEV_ID, ZipfProbBoundary> entry : devID_ProbBoundaryMap.entrySet())
        {
            if(entry.getValue().isInsideBoundary(randDouble))
                return entry.getKey();
        }

        assert(false); // code should not reach to this line
        return null; // key not found in map... something is wrong;
    }

    private static String prepareZipfian()
    {
        assert(0 < devIDlist.size());

        int numberOfElements = devIDlist.size();

        ZipfDistribution zipf = new ZipfDistribution(numberOfElements, zipfCoefficient);

        List<Double> cumulativeProbabilityList = new ArrayList<>();

        for(int I = 0 ; I < devIDlist.size() ; I++)
        {
            double probability = zipf.probability(I + 1);

            if(I == 0)
                cumulativeProbabilityList.add(probability);
            else
                cumulativeProbabilityList.add(probability + cumulativeProbabilityList.get(I - 1));
        }

        //System.out.println(cumulativeProbabilityList);

        double lowerInclusive = 0.0;

        for(int I = 0 ; I < devIDlist.size() ; I++)
        {
            double upperExclusive = cumulativeProbabilityList.get(I);

            if(I == devIDlist.size() - 1)
                upperExclusive = 1.01;

            //System.out.println( "item " + I + " lowerInclusive = " + lowerInclusive + " upperExclusive = " + upperExclusive );

            ZipfProbBoundary zipfProbBoundary = new ZipfProbBoundary(lowerInclusive, upperExclusive);
            devID_ProbBoundaryMap.put( devIDlist.get(I) ,zipfProbBoundary);

            lowerInclusive = cumulativeProbabilityList.get(I);
        }


        Random rand = new Random();

        Map<DEV_ID, Integer> histogram = new HashMap<>();

        Double sampleSize = 1000000.0;
        for(int I = 0 ; I < sampleSize ; I++)
        {
            DEV_ID devId = getZipfDistDevID(rand.nextDouble());
            if(!histogram.containsKey(devId))
                histogram.put(devId, 0);

            histogram.put(devId, histogram.get(devId) + 1 );
        }

        String str = "";

        for(DEV_ID devId: devIDlist)
        {
            if(histogram.containsKey(devId))
            {
                Double percentage = (histogram.get(devId) / sampleSize) * 100.0;
                String formattedStr = String.format("%s -> selection probability = %.2f%%", devId.name(), percentage);
                str += formattedStr + "\n";
            }

        }


        //System.out.println(str);

        return str;
    }


    private static int ROUTINE_ID = 0;

    public static int getUniqueRtnID()
    {
        return Temp.ROUTINE_ID++;
    }

    private static List<Routine> generateAutomatedRtn(int nonNegativeSeed)
    {
        List<Routine> routineList = new ArrayList<>();
        Random rand;

        if(0 <= nonNegativeSeed)
            rand = new Random(nonNegativeSeed);
        else
            rand = new Random();

        int totalConcurrentRtn = maxConcurrentRtn;

        int longRunningRoutineCount = 0;

        for(int RoutineCount = 0 ; RoutineCount < totalConcurrentRtn ; ++RoutineCount)
        {
            double nextDbl = rand.nextDouble();
            nextDbl = (nextDbl == 1.0) ? nextDbl - 0.001 : nextDbl;
            boolean isLongRunning = (nextDbl < longRunningRtnPercentage);

            if(isLongRunning)
                longRunningRoutineCount++;

            if(atleastOneLongRunning && (RoutineCount == totalConcurrentRtn - 1) && longRunningRoutineCount == 0)
            {
                isLongRunning = true; // at least one routine will be long running;
            }

            int totalCommandInThisRtn = maxCommandPerRtn;

            assert(totalCommandInThisRtn <= devIDlist.size());

            Map<DEV_ID, Integer> devIDDurationMap = new HashMap<>();
            List<DEV_ID> devList = new ArrayList<>();

            while(devIDDurationMap.size() < totalCommandInThisRtn)
            {
                DEV_ID devID;

                /*
                //SBA: this method is not working well. instead, always choose the long running from the zipf
                if(isLongRunning)
                {// for long running, we will select the device from Uniform distribution
                    devID = getZipfDistDevID(rand.nextDouble());
                    //DEV_ID zipfDistDev = getZipfDistDevID(rand.nextDouble());
                }
                else
                {
                    // for sort running, we will follow the zipf distribution
                    devID = devIDlist.get( rand.nextInt(devIDlist.size()) );
                    //DEV_ID randDev = devIDlist.get( rand.nextInt(devIDlist.size()) );
                }
                */
                devID = getZipfDistDevID(rand.nextDouble());

                if(devIDDurationMap.containsKey(devID))
                    continue;

                int duration;
                int currentDurationMapSize = devIDDurationMap.size();
                int middleCommandIndex = totalCommandInThisRtn / 2;
                if(isLongRunning && ( currentDurationMapSize == middleCommandIndex) )
                { // select the  middle command as long running command

                    if(isLongCmdDurationVary)
                    {
                        int durationUpperBoundExclusive = (int)Math.ceil(longRunningCmdDuration*longCmdDurationVaryMultiplier);
                        duration = longRunningCmdDuration + rand.nextInt(durationUpperBoundExclusive) ;
                    }
                    else
                    {
                        duration = longRunningCmdDuration;
                    }
                }
                else
                {
                    if(isShortCmdDurationVary)
                    {
                        int durationUpperBoundExclusive = (int)Math.ceil(shortCmdDuration*shortCmdDurationVaryMultiplier);
                        duration = shortCmdDuration + rand.nextInt(durationUpperBoundExclusive);
                    }
                    else
                    {
                        duration = shortCmdDuration;
                    }
                }

                //System.out.println(randDev.name() + " " + duration);

                devIDDurationMap.put(devID, duration);
                devList.add(devID);
            }
            //System.out.println("===");

            Routine rtn = new Routine();

            for(DEV_ID devID : devList)
            {
                assert(devIDDurationMap.containsKey(devID));

                nextDbl = rand.nextDouble();
                nextDbl = (nextDbl == 1.0) ? nextDbl - 0.001 : nextDbl;
                boolean isMust = (nextDbl < mustCmdPercentage);
                Command cmd = new Command(devID, devIDDurationMap.get(devID), isMust);
                //System.out.println("@ " + devID.name() + " => " + devIDDurationMap.get(devID));
                rtn.addCommand(cmd);
            }
            routineList.add(rtn);
        }

        Collections.shuffle(routineList, rand);

        if(isSentAllRtnSameTime)
        {
            for(int index = 0 ; index < routineList.size() ; ++index)
            {
                routineList.get(index).registrationTime = SIMULATION_START_TIME;
            }
        }
        else
        {
            double allRtnBackToBackExcTime = 0.0;
            for(Routine rtn : routineList)
            {
                allRtnBackToBackExcTime += rtn.getBackToBackCmdExecutionTimeWithoutGap();
            }

            double simulationLastRtnStartTime = allRtnBackToBackExcTime * timelineMultiplierForSporadicRoutines;

            int upperLimit = (int)Math.ceil(simulationLastRtnStartTime);

            List<Integer> randStartPointList = new ArrayList<>();
            for(int I = 0 ; I < routineList.size() ; I++)
            {
                int randStartPoint = SIMULATION_START_TIME + rand.nextInt(upperLimit);
                randStartPointList.add(randStartPoint);
            }

            Collections.sort(randStartPointList);

            for(int I = 0 ; I < routineList.size() ; I++)
            {
                routineList.get(I).registrationTime = randStartPointList.get(I);
            }
        }

        for(int index = 0 ; index < routineList.size() ; ++index)
        {
            routineList.get(index).ID = getUniqueRtnID();
        }


        return routineList;
    }

    private static List<Routine> generateFixedPatternRtn(int nonNegativeSeed)
    {
        List<Routine> routineList = new ArrayList<>();
        Random rand;

        if(0 <= nonNegativeSeed)
        {
            rand = new Random(nonNegativeSeed);
        }
        else
        {
            rand = new Random();
        }

        int totalConcurrentRtn = maxConcurrentRtn;
        int longRunningRoutineID = totalConcurrentRtn/2; // select the middle routine as long running routine

        for(int RoutineCount = 0 ; RoutineCount < totalConcurrentRtn ; ++RoutineCount)
        {
            int totalCommandInThisRtn = maxCommandPerRtn;
            int middleCommandIndex = totalCommandInThisRtn / 2;
            assert(totalCommandInThisRtn <= devIDlist.size());

            Map<DEV_ID, Integer> devIDDurationMap = new HashMap<>();

            List<DEV_ID> devList = new ArrayList<>();

            while(devIDDurationMap.size() < totalCommandInThisRtn)
            {
                DEV_ID randDev = devIDlist.get( rand.nextInt(devIDlist.size()) );

                if(devIDDurationMap.containsKey(randDev))
                    continue;

                int duration = shortCmdDuration;

                int currentDurationMapSize = devIDDurationMap.size();
                if((RoutineCount == longRunningRoutineID) && ( currentDurationMapSize == middleCommandIndex) )
                { // select the middle routine's middle command as long running command
                    duration = longRunningCmdDuration;
                }

                //System.out.println(randDev.name() + " " + duration);

                devIDDurationMap.put(randDev, duration);
                devList.add(randDev);
            }
            //System.out.println("===");

            Routine rtn = new Routine();

            for(DEV_ID devID : devList)
            {
                assert(devIDDurationMap.containsKey(devID));

                boolean isMust = true;
                Command cmd = new Command(devID, devIDDurationMap.get(devID), isMust);
                //System.out.println("@ " + devID.name() + " => " + devIDDurationMap.get(devID));
                rtn.addCommand(cmd);
            }
//            for(Map.Entry<DEV_ID, Integer> entry : devIDDurationMap.entrySet())
//            {
//                Command cmd = new Command(entry.getKey(), entry.getValue());
//                System.out.println("@ " + entry.getKey().name() + " => " + entry.getValue());
//                rtn.addCommand(cmd);
//            }
            routineList.add(rtn);
        }

        return routineList;
    }

    private static String printInitialRoutineList(List<Routine> routineList)
    {
        String logStr = "";
        for(Routine rtn : routineList)
        {
            System.out.println("# " + rtn);
            logStr += "#" + rtn + "\n";
        }
        return logStr;
    }



    public static ExpResults runExperiment(List<DEV_ID> _devIDlist, CONSISTENCY_TYPE _consistencyType, final List<Routine> _originalRtnList, int _simulationStartTime)
    {
        LockTable lockTable = new LockTable(_devIDlist, _consistencyType);

        List<Routine> perExpRtnList = new ArrayList<>();
        for(Routine originalRtn: _originalRtnList)
        {
            perExpRtnList.add(originalRtn.getDeepCopy());
        }


        lockTable.register(perExpRtnList, _simulationStartTime);

        ExpResults expResults = new ExpResults();

        if(_consistencyType != CONSISTENCY_TYPE.WEAK)
            expResults.failureAnalyzer = new FailureAnalyzer(lockTable.lockTable, _consistencyType);

        expResults.measurement = new Measurement(lockTable, _consistencyType);


        for(Routine routine : perExpRtnList)
        {
            if(isPrint) System.out.println(routine);
            expResults.waitTimeList.add(routine.getStartDelay());
            expResults.endToEndLatencyList.add(routine.getLatencyOverheadPrcnt());
            expResults.stretchRatioList.add(routine.getStretchRatio());
        }

        return expResults;
    }



    public static void main (String[] args)
    {
        devIDlist.add(DEV_ID.A);
        devIDlist.add(DEV_ID.B);
        devIDlist.add(DEV_ID.C);
        devIDlist.add(DEV_ID.D);
        devIDlist.add(DEV_ID.E);
        devIDlist.add(DEV_ID.F);
        devIDlist.add(DEV_ID.G);
//        devIDlist.add(DEV_ID.H);
//        devIDlist.add(DEV_ID.I);
//        devIDlist.add(DEV_ID.J);
//        devIDlist.add(DEV_ID.K);
//        devIDlist.add(DEV_ID.L);
//        devIDlist.add(DEV_ID.M);
//        devIDlist.add(DEV_ID.N);
//        devIDlist.add(DEV_ID.O);


        //////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////---CHECKING-DIRECTORY-///////////////////////////////
        String dataStorageDirectory = "C:\\Users\\shegufta\\Desktop\\smartHomeData";

        File dataStorageDir = new File(dataStorageDirectory);

        if(!dataStorageDir.exists())
        {
            System.out.println("\n ERROR: directory not found: " + dataStorageDirectory);
            System.exit(1);
        }

        //////////////////////////////////////////////////////////////////////////////////


        String logStr = "";


        MeasurementCollector measurementCollector = new MeasurementCollector(MAX_DATAPOINT_COLLECTON_SIZE);
        Map<Double, List<Double>> globalDataCollector = new HashMap<>();
        List<Double> variableTrakcer = new ArrayList<>();
        Double changingParameterValue = -1.0;

        ///////////////////////////
        double lastGeneratedZipfeanFor = Double.MAX_VALUE;
//        String zipFianStr = prepareZipfian(); // we dont need to uncomment this line anymore. it has been taken care of inside the for loop
//        System.out.println(zipFianStr);
//        logStr += zipFianStr;
        ///////////////////////////

        final String changingParameterName = "maxConcurrentRtn"; // NOTE: also change changingParameterValue
        for(maxConcurrentRtn = 50; maxConcurrentRtn <= 50 ; maxConcurrentRtn++)
        {
            changingParameterValue = (double)maxConcurrentRtn; // NOTE: also change changingParameterName

            if(lastGeneratedZipfeanFor != zipfCoefficient)
            {
                lastGeneratedZipfeanFor = zipfCoefficient;
                //dont need to comment-on/off for variable zipfean.If zipfean varies, it will automatically log it and prepare the zipfean for each run.
                ///////////////////////////
                String zipFianStr = prepareZipfian();
                System.out.println(zipFianStr);
                logStr += zipFianStr;
                ///////////////////////////
            }
            variableTrakcer.add(changingParameterValue); // add the variable name
            List<Double> resultCollector = new ArrayList<>();


            System.out.println("--------------------------------");
            logStr += "--------------------------------\n";
            System.out.println("Total Device Count: = " + devIDlist.size());
            logStr += "Total Device Count: = " + devIDlist.size() + "\n";

            System.out.println("totalConcurrentRtn = " + maxConcurrentRtn);
            logStr += "totalConcurrentRtn = " + maxConcurrentRtn + "\n";
            System.out.println("maxCommandPerRtn = " + maxCommandPerRtn);
            logStr += "maxCommandPerRtn = " + maxCommandPerRtn + "\n";
            System.out.println("isSentAllRtnSameTime = " + isSentAllRtnSameTime);
            logStr += "isSentAllRtnSameTime = " + isSentAllRtnSameTime + "\n";
            System.out.println("isSentAllRtnSameTime = " + timelineMultiplierForSporadicRoutines + " (works only if isSentAllRtnSameTime == false)");
            logStr += "isSentAllRtnSameTime = " + timelineMultiplierForSporadicRoutines + " (works only if isSentAllRtnSameTime == false)\n";


            System.out.println("zipfCoefficient = " + zipfCoefficient);
            logStr += "zipfCoefficient = " + zipfCoefficient + "\n";

            System.out.println("devFailureRatio = " + devFailureRatio);
            logStr += "devFailureRatio = " + devFailureRatio + "\n";
            System.out.println("atleastOneDevFail = " + atleastOneDevFail);
            logStr += "atleastOneDevFail = " + atleastOneDevFail + "\n";

            System.out.println("mustCmdPercentage = " + mustCmdPercentage);
            logStr += "mustCmdPercentage = " + mustCmdPercentage + "\n";

            System.out.println("IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED);
            logStr += "IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED + "\n";

            System.out.println("IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED);
            logStr += "IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED + "\n";


            System.out.println("shortCmdDuration = " + shortCmdDuration);
            logStr += "shortCmdDuration = " + shortCmdDuration + "\n";
            System.out.println("isShortCmdDurationVary = " + isShortCmdDurationVary);
            logStr += "isShortCmdDurationVary = " + isShortCmdDurationVary + "\n";
            System.out.println("shortCmdDurationVaryMultiplier = " + shortCmdDurationVaryMultiplier);
            logStr += "shortCmdDurationVaryMultiplier = " + shortCmdDurationVaryMultiplier + "\n";

            System.out.println("longRunningCmdDuration = " + longRunningCmdDuration);
            logStr += "longRunningCmdDuration = " + longRunningCmdDuration + "\n";
            System.out.println("isLongCmdDurationVary = " + isLongCmdDurationVary);
            logStr += "isLongCmdDurationVary = " + isLongCmdDurationVary + "\n";
            System.out.println("longCmdDurationVaryMultiplier = " + longCmdDurationVaryMultiplier);
            logStr += "longCmdDurationVaryMultiplier = " + longCmdDurationVaryMultiplier + "\n";
            System.out.println("longRunningRtnPercentage = " + longRunningRtnPercentage);
            logStr += "longRunningRtnPercentage = " + longRunningRtnPercentage + "\n";
            System.out.println("atleastOneLongRunning = " + atleastOneLongRunning);
            logStr += "atleastOneLongRunning = " + atleastOneLongRunning + "\n";

            System.out.println("weakVisibilityDeviceAccessTime = " + weakVisibilityDeviceAccessTime);
            logStr += "weakVisibilityDeviceAccessTime = " + weakVisibilityDeviceAccessTime + "\n";

            System.out.println("SIMULATION_START_TIME = " + SIMULATION_START_TIME);
            logStr += "SIMULATION_START_TIME = " + SIMULATION_START_TIME + "\n";

            System.out.println("totalSampleCount = " + totalSampleCount);
            logStr += "totalSampleCount = " + totalSampleCount + "\n";

            System.out.println("--------------------------------");
            logStr += "--------------------------------\n";

            int resolution = 10;
            int stepSize = totalSampleCount / resolution;
            if(stepSize == 0)
                stepSize = 1;

            for(int I = 0 ; I < totalSampleCount ; I++)
            {
                if(I == totalSampleCount - 1)
                {
                    System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + " 100%");
                }
                else if(totalSampleCount % stepSize == 0)
                {
                    System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + (int) (100.0 * ((double)I / (double)totalSampleCount)) + "%");
                }


                final List<Routine> routineSet = generateAutomatedRtn(-1);


                FailureResult failureResult;
                ExpResults expResult;
                double abtRatio;
                double recoverCmdRatio;

///////////////////////////////////////-------STRONG------////////////////////////////////////////////////////////////
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.STRONG, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                failureResult = expResult.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ABORT_RATE,
                        abtRatio, true);

                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.RECOVERY_CMD,
                        recoverCmdRatio, true);


                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////-------RELAXED STRONG-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.RELAXED_STRONG, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                failureResult = expResult.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ABORT_RATE,
                        abtRatio, true);

                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.RECOVERY_CMD,
                        recoverCmdRatio, true);


                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////-------WEAK-----/////////////////////////////////////////////////////////
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.WEAK, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);



                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------EVENTUAL-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.EVENTUAL, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.STRETCH_RATIO, expResult.stretchRatioList);

                failureResult = expResult.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ABORT_RATE,
                        abtRatio, true);

                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.RECOVERY_CMD,
                        recoverCmdRatio, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////-------LAZY-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------LAZY-FCFS-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY_FCFS, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------LAZY-PRIORITY-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY_PRIORITY, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeList);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.endToEndLatencyList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallalRtnCntList);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT,
                        expResult.measurement.isvltn_perRtnVictimCmdPrcntList);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN,
                        expResult.measurement.isvltn_victimRtnPercentPerRun, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, false);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPercentList);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            }

            logStr += "\n=========================================================================\n";



            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.WAIT_TIME));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.STRETCH_RATIO));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ABORT_RATE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ABORT_RATE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ABORT_RATE));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.RECOVERY_CMD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.RECOVERY_CMD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.RECOVERY_CMD));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.PARALLEL));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            ////////////////////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            /////////////////////////////////////////////////////////////

            globalDataCollector.put(changingParameterValue, resultCollector);


        }

        String globalResult = "\n--------------------------------\n";
        //String header = "Variable\tStgAvg\tStgSD\tR.StgAvg\tR.StgSD\tLzyAvg\tLzySD\tEvnAvg\tEvnSD\tEvnStretchRatioAvg\tEvnStretchRatioSD";
        String header = "G0:Variable";

        header += "\tG1:GSV_WaitTm\tG1:PSV_WaitTm\tG1:WV_WaitTm\tG1:EV_WaitTm\tG1:LV_WaitTm\tG1:FCFSV_WaitTm\tG1:LzPRIOTY_WaitTm";
        header += "\tG2:GSV_LtncyOvrhdPcnt\tG2:PSV_LtncyOvrhdPcnt\tG2:WV_LtncyOvrhdPcnt\tG2:EV_LtncyOvrhdPcnt\tG2:LV_LtncyOvrhdPcnt\tG2:FCFSV_LtncyOvrhdPcnt\tG2:LzPRIOTY_LtncyOvrhdPcnt";
        header += "\tG3:EV_Stretch";

        header += "\tG4:GSV_Abort\tG4:PSV_Abort\tG4:EV_Abort";
        header += "\tG5:GSV_RcvryCmdRatio\tG5:PSV_RcvryCmdRatio\tG5:EV_RcvryCmdRatio";

        header += "\tG6:GSV_Parrl\tG6:PSV_Parrl\tG6:WV_Parrl\tG6:EV_Parrl\tG6:LV_Parrl\tG6:FCFSV_Parrl\tG6:LzPRIOTY_Parrl";
        header += "\tG7:GSV_OdrMismtch\tG7:PSV_OdrMismtch\tG7:WV_OdrMismtch\tG7:EV_OdrMismtch\tG7:LV_OdrMismtch\tG7:FCFSV_OdrMismtch\tG7:LzPRIOTY_OdrMismtch";
        header += "\tG8:GSV_DevUtlz\tG8:PSV_DevUtlz\tG8:WV_DevUtlz\tG8:EV_DevUtlz\tG8:LV_DevUtlz\tG8:FCFSV_DevUtlz\tG8:LzPRIOTY_DevUtlz";

        header += "\tG9:GSV_IsvltnPerVctmUniqAttckrCnt\tG9:PSV_IsvltnPerVctmUniqAttckrCnt\tG9:WV_IsvltnPerVctmUniqAttckrCnt\tG9:EV_IsvltnPerVctmUniqAttckrCnt\tG9:LV_IsvltnPerVctmUniqAttckrCnt\tG9:FCFSV_IsvltnPerVctmUniqAttckrCnt\tG9:LzPRIOTY_IsvltnPerVctmUniqAttckrCnt";
        header += "\tG10:GSV_IsvltnVctmRtnPrcntPerRun\tG10:PSV_IsvltnVctmRtnPrcntPerRun\tG10:WV_IsvltnVctmRtnPrcntPerRun\tG10:EV_IsvltnVctmRtnPrcntPerRun\tG10:LV_IsvltnVctmRtnPrcntPerRun\tG10:FCFSV_IsvltnVctmRtnPrcntPerRun\tG10:LzPRIOTY_IsvltnVctmRtnPrcntPerRun";
        header += "\tG11:GSV_IsvltnPerRtnVctmCmdPrcnt\tG11:PSV_IsvltnPerRtnVctmCmdPrcnt\tG11:WV_IsvltnPerRtnVctmCmdPrcnt\tG11:EV_IsvltnPerRtnVctmCmdPrcnt\tG11:LV_IsvltnPerRtnVctmCmdPrcnt\tG11:FCFSV_IsvltnPerRtnVctmCmdPrcnt\tG11:LzPRIOTY_IsvltnPerRtnVctmCmdPrcnt";

        globalResult += header + "\n";
        for(double variable : variableTrakcer)
        {
            globalResult += variable + "\t";

            for(double stats : globalDataCollector.get(variable))
            {
                String formattedNumber = String.format("%.3f", stats);
                globalResult += stats + "\t";

//                if(stats < 1.0)
//                {
//                    String formattedNumber = String.format("%.3f", stats);
//                    globalResult += formattedNumber + "\t";
//                }
//                else
//                {
//                    globalResult += stats + "\t";
//                }
            }

            globalResult += "\n";
        }
        globalResult += "--------------------------------\n";

        logStr += globalResult;


        ////////////////////-CREATING-SUBDIRECTORY-/////////////////////////////
        String epoch = System.currentTimeMillis() + "";
        String parentDirPath = dataStorageDirectory + "\\" + epoch + "_VARY_"+ changingParameterName;
        parentDirPath += "_R_" + maxConcurrentRtn + "_C_" + maxCommandPerRtn;

        File parentDir = new File(parentDirPath);
        if(!parentDir.exists())
        {
            parentDir.mkdir();
        }
        ////////////////////////////////////////////////////////////////

        try
        {
            String fileName = "VARY_" + changingParameterName + ".dat";
            String filePath = parentDirPath + "\\" + fileName;

            Writer fileWriter = new FileWriter(filePath);
            fileWriter.write(logStr);
            fileWriter.close();

        } catch (IOException e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }

        System.out.println("\n\nPROCESSING.....");
        measurementCollector.writeStatsInFile(parentDirPath, changingParameterName);
        System.out.println(globalResult);


    }

}
