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
    public static final int MAX_DATAPOINT_COLLECTON_SIZE = 5000;

    public static final boolean IS_PRE_LEASE_ALLOWED = true;
    public static final boolean IS_POST_LEASE_ALLOWED = true;

    private static int maxCommandPerRtn = 3; // in current version totalCommandInThisRtn = maxCommandPerRtn;
    private static int maxConcurrentRtn = 5; //in current version totalConcurrentRtn = maxConcurrentRtn;
    private static double zipfCoefficient = 0.01;
    private static double longRunningRtnPercentage = 0.0;
    private static final boolean atleastOneLongRunning = true;

    private static double devFailureRatio = 0.0;
    private static final boolean atleastOneDevFail = false;
    private static double mustCmdPercentage = 1.0;

    private static int weakVisibilityDeviceAccessTime = 1;

    private static int longRunningCmdDuration = 200;
    private static final boolean isLongCmdDurationVary = false;
    private static final int longCmdDurationVaryMultiplier = 1; // will vary upto N times

    private static final int shortCmdDuration = 5;
    private static final boolean isShortCmdDurationVary = true;
    private static final int shortCmdDurationVaryMultiplier = 3; // will vary upto N times

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
                        duration = longRunningCmdDuration + rand.nextInt(longRunningCmdDuration*longCmdDurationVaryMultiplier) ;
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
                        duration = shortCmdDuration + rand.nextInt(shortCmdDuration*shortCmdDurationVaryMultiplier);
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



    public static ExpResults runExperiment(List<DEV_ID> _devIDlist, CONSISTENCY_TYPE _consistencyType, final List<Routine> _originalRtnList, int currentTime)
    {
        LockTable lockTable = new LockTable(_devIDlist, _consistencyType);

        List<Routine> perExpRtnList = new ArrayList<>();
        for(Routine originalRtn: _originalRtnList)
        {
            perExpRtnList.add(originalRtn.getDeepCopy());
        }

//        if(_consistencyType == CONSISTENCY_TYPE.WEAK)
//        {
//            for(int R = 0 ; R < perExpRtnList.size() ; R++)
//            {
//                for(int C = 0 ; C < perExpRtnList.get(R).commandList.size() ; C++)
//                {// for weak visibility, we only care how much time it takes to get the corresponding ACK from the device
//                    //e.g.  Turn-on-tv ->  ACK. We dont care about the actual duration.
//                    perExpRtnList.get(R).commandList.get(C).overrideDurationForWeakVisibilityModel(weakVisibilityDeviceAccessTime);
//                }
//            }
//        }

        lockTable.register(perExpRtnList, currentTime);

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
        devIDlist.add(DEV_ID.H);
        devIDlist.add(DEV_ID.I);
        devIDlist.add(DEV_ID.J);
        devIDlist.add(DEV_ID.K);
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
        String zipFianStr = prepareZipfian();
        System.out.println(zipFianStr);
        logStr += zipFianStr;
        ///////////////////////////

        final String changingParameterName = "maxConcurrentRtn"; // NOTE: also change changingParameterValue
        for(maxConcurrentRtn = 1; maxConcurrentRtn <= 10 ; maxConcurrentRtn++)
        {
            changingParameterValue = (double)maxConcurrentRtn; // NOTE: also change changingParameterName

            ///////////////////////////
//            String zipFianStr = prepareZipfian();
//            System.out.println(zipFianStr);
//            logStr += zipFianStr;
            ///////////////////////////

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


                List<Routine> routineSet = generateAutomatedRtn(-1);


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
                        expResult.measurement.isvltn_victimRtnPercentPerRun, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, true);

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
                        expResult.measurement.isvltn_victimRtnPercentPerRun, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, true);

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
                        expResult.measurement.isvltn_victimRtnPercentPerRun, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, true);

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
                        expResult.measurement.isvltn_victimRtnPercentPerRun, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, true);

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
                        expResult.measurement.isvltn_victimRtnPercentPerRun, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT,
                        expResult.measurement.isvltn_totalUniqueAttackerPerRoutineList);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderMismatchPercent, true);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
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
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
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
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            ////////////////////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_VICTIM_RTN_ATTCKER_COUNT));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_VICTIM_RTN_PRCNT_PER_RUN));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN_PER_RTN_VICTIM_CMD_PRCNT));
            /////////////////////////////////////////////////////////////

            globalDataCollector.put(changingParameterValue, resultCollector);


        }

        String globalResult = "\n--------------------------------\n";
        //String header = "Variable\tStgAvg\tStgSD\tR.StgAvg\tR.StgSD\tLzyAvg\tLzySD\tEvnAvg\tEvnSD\tEvnStretchRatioAvg\tEvnStretchRatioSD";
        String header = "Variable";

        header += "\tGSV_WaitTm\tPSV_WaitTm\tWV_WaitTm\tEV_WaitTm\tLV_WaitTm";
        header += "\tGSV_LtncyOvrhdPcnt\tPSV_LtncyOvrhdPcnt\tWV_LtncyOvrhdPcnt\tEV_LtncyOvrhdPcnt\tLV_LtncyOvrhdPcnt";
        header += "\tEV_Stretch";

        header += "\tGSV_Abort\tPSV_Abort\tEV_Abort";
        header += "\tGSV_RcvryCmdRatio\tPSV_RcvryCmdRatio\tEV_RcvryCmdRatio";

        header += "\tGSV_Parrl\tPSV_Parrl\tWV_Parrl\tEV_Parrl\tLV_Parrl";
        header += "\tGSV_OdrMismtch\tPSV_OdrMismtch\tWV_OdrMismtch\tEV_OdrMismtch\tLV_OdrMismtch";
        header += "\tGSV_DevUtlz\tPSV_DevUtlz\tWV_DevUtlz\tEV_DevUtlz\tLV_DevUtlz";

        header += "\tGSV_IsvltnPerVctmUniqAttckrCnt\tPSV_IsvltnPerVctmUniqAttckrCnt\tWV_IsvltnPerVctmUniqAttckrCnt\tEV_IsvltnPerVctmUniqAttckrCnt\tLV_IsvltnPerVctmUniqAttckrCnt";
        header += "\tGSV_IsvltnVctmRtnPrcntPerRun\tPSV_IsvltnVctmRtnPrcntPerRun\tWV_IsvltnVctmRtnPrcntPerRun\tEV_IsvltnVctmRtnPrcntPerRun\tLV_IsvltnVctmRtnPrcntPerRun";
        header += "\tGSV_IsvltnPerRtnVctmCmdPrcnt\tPSV_IsvltnPerRtnVctmCmdPrcnt\tWV_IsvltnPerRtnVctmCmdPrcnt\tEV_IsvltnPerRtnVctmCmdPrcnt\tLV_IsvltnPerRtnVctmCmdPrcnt";

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
