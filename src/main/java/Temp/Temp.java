package Temp;


import BenchmarkingTool.*;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static Temp.SysParamSngltn.IS_RUNNING_BENCHMARK;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 17-Jul-19
 * @time 10:32 AM
 */
// graph plotting tool command:  "python .\gen_all.py -d C:\Users\shegufta\Desktop\smartHomeData\1568337471715_VARY_maxConcurrentRtn_R_101_C_6"

public class Temp
{
    public static final boolean IS_RUNNIGN_BENCHMARK = SysParamSngltn.getInstance().IS_RUNNING_BENCHMARK; //false; // Careful... if it is TRUE, all other parameters will be in don't care mode!
    private static final int totalSampleCount = SysParamSngltn.getInstance().totalSampleCount; //1000;//7500;//10000; // 100000;

    private static double shrinkFactor = SysParamSngltn.getInstance().shrinkFactor; // 0.25; // shrink the total time... this parameter controls the concurrency
    private static double minCmdCntPerRtn = SysParamSngltn.getInstance().minCmdCntPerRtn; //  1;
    private static double maxCmdCntPerRtn = SysParamSngltn.getInstance().maxCmdCntPerRtn; //  3;

    private static double zipF = SysParamSngltn.getInstance().zipF; //  0.01;
    public static int devRegisteredOutOf65Dev = SysParamSngltn.getInstance().devRegisteredOutOf65Dev;
    private static int maxConcurrentRtn = SysParamSngltn.getInstance().maxConcurrentRtn; //  100; //in current version totalConcurrentRtn = maxConcurrentRtn;

    private static double longRrtnPcntg = SysParamSngltn.getInstance().longRrtnPcntg; //  0.1;
    private static final boolean isAtleastOneLongRunning = SysParamSngltn.getInstance().isAtleastOneLongRunning; //  false;
    private static double minLngRnCmdTimSpn = SysParamSngltn.getInstance().minLngRnCmdTimSpn; //  2000;
    private static double maxLngRnCmdTimSpn = SysParamSngltn.getInstance().maxLngRnCmdTimSpn; //  minLngRnCmdTimSpn * 2;

    private static final double minShrtCmdTimeSpn = SysParamSngltn.getInstance().minShrtCmdTimeSpn; //  10;
    private static final double maxShrtCmdTimeSpn = SysParamSngltn.getInstance().maxShrtCmdTimeSpn; //  minShrtCmdTimeSpn * 6;

    private static double devFailureRatio = SysParamSngltn.getInstance().devFailureRatio; //  0.0;
    private static final boolean atleastOneDevFail = SysParamSngltn.getInstance().atleastOneDevFail; //  false;
    private static double mustCmdPercentage = SysParamSngltn.getInstance().mustCmdPercentage; //  1.0;

    public static final boolean IS_PRE_LEASE_ALLOWED = SysParamSngltn.getInstance().IS_PRE_LEASE_ALLOWED; // true;
    public static final boolean IS_POST_LEASE_ALLOWED = SysParamSngltn.getInstance().IS_POST_LEASE_ALLOWED; // true;

    private static final int SIMULATION_START_TIME = SysParamSngltn.getInstance().SIMULATION_START_TIME; //  0;
    public static final int MAX_DATAPOINT_COLLECTON_SIZE = SysParamSngltn.getInstance().MAX_DATAPOINT_COLLECTON_SIZE; //  5000;
    private static final int RANDOM_SEED = SysParamSngltn.getInstance().RANDOM_SEED; //  -1;
    private static final int MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = SysParamSngltn.getInstance().MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING; //  5;

    private static final String dataStorageDirectory = SysParamSngltn.getInstance().dataStorageDirectory; //  "C:\\Users\\shegufta\\Desktop\\smartHomeData";

    private static List<DEV_ID> devIDlist = new ArrayList<>();
    private static Map<DEV_ID, ZipfProbBoundary> devID_ProbBoundaryMap = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////////////
    public static Map<CONSISTENCY_TYPE, String> CONSISTENCY_HEADER = new HashMap<>();
    public static List<CONSISTENCY_TYPE> CONSISTENCY_ORDERING_LIST = new ArrayList<>();
    ///////////////////////////////////////////////////////////////////////////////////

    public Temp()
    {

    }


    private static void initiateSyntheticDevices()
    {
        int count = devRegisteredOutOf65Dev;
        int totalAvailable = DEV_ID.values().length;

        count = Math.min(count, totalAvailable);

        for(DEV_ID devID : DEV_ID.values())
        {
            count--;
            devIDlist.add(devID);

            if(count <= 0)
                break;
        }
    }


    public static void main (String[] args) throws Exception
    {
        Benchmark benchmarkingTool = null;

        if(IS_RUNNIGN_BENCHMARK)
        {
            benchmarkingTool = new Benchmark(RANDOM_SEED, MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING);
            benchmarkingTool.initiateDevices(devIDlist);
        }
        else
        {
            initiateSyntheticDevices();
        }

        //////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////---CHECKING-DIRECTORY-///////////////////////////////

        File dataStorageDir = new File(dataStorageDirectory);

        if(!dataStorageDir.exists())
        {
            System.out.println("\n ERROR: directory not found: " + dataStorageDirectory);
            System.exit(1);
        }
        //////////////////////////////////////////////////////////////////////////////////

        String logStr = "";


        MeasurementCollector measurementCollector = new MeasurementCollector(MAX_DATAPOINT_COLLECTON_SIZE);

        //Map<Double, List<Float>> globalDataCollector = new HashMap<>();
        Map<Double, Map<MEASUREMENT_TYPE, Map<CONSISTENCY_TYPE, Double>>> globalDataCollector = new LinkedHashMap<>();

        List<Double> variableTrakcer = new ArrayList<>();
        Double changingParameterValue = -1.0;
        double lastGeneratedZipfeanFor = Double.MAX_VALUE; // NOTE: declare zipfean here... DO NOT declare it inside the for loop!

        ////////////////////////////////////////////////////////////////////////////////
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.STRONG, "GSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.RELAXED_STRONG, "PSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.EVENTUAL, "EV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.WEAK, "WV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY, "LV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_FCFS, "LAZY_FCFS");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_PRIORITY, "LAZY_PRIORITY");

        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.STRONG);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.RELAXED_STRONG);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.EVENTUAL);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.WEAK);
//        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.LAZY);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.LAZY_FCFS);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.LAZY_PRIORITY);
        ////////////////////////////////////////////////////////////////////////////////
        List<MEASUREMENT_TYPE> measurementList = new ArrayList<>();
        measurementList.add(MEASUREMENT_TYPE.WAIT_TIME);
        measurementList.add(MEASUREMENT_TYPE.LATENCY_OVERHEAD);
        measurementList.add(MEASUREMENT_TYPE.STRETCH_RATIO);
//            measurementList.add(MEASUREMENT_TYPE.ABORT_RATE);
//            measurementList.add(MEASUREMENT_TYPE.RECOVERY_CMD);
        measurementList.add(MEASUREMENT_TYPE.PARALLEL);
        measurementList.add(MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE);
        measurementList.add(MEASUREMENT_TYPE.DEVICE_UTILIZATION);
        measurementList.add(MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT);
        measurementList.add(MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT);
        measurementList.add(MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN);
        measurementList.add(MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT);
        measurementList.add(MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT);

//        List<CONSISTENCY_TYPE> consistencyList = new ArrayList<>();
//        consistencyList.add(CONSISTENCY_TYPE.STRONG);
//        consistencyList.add(CONSISTENCY_TYPE.RELAXED_STRONG);
//        consistencyList.add(CONSISTENCY_TYPE.WEAK);
//        consistencyList.add(CONSISTENCY_TYPE.EVENTUAL);
//        consistencyList.add(CONSISTENCY_TYPE.LAZY_FCFS);
//        consistencyList.add(CONSISTENCY_TYPE.LAZY_PRIORITY);
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        List<Double> variableList = new ArrayList<>();


//        variableList.add(0.1);
//        variableList.add(0.2);
//        variableList.add(0.3);
//        variableList.add(0.4);
//        variableList.add(0.5);
//        variableList.add(0.6);
//        variableList.add(0.7);
//        variableList.add(0.8);
//        variableList.add(0.9);
//        variableList.add(0.0);
//        variableList.add(0.05);
//        variableList.add(0.1);
//        variableList.add(0.15);
//        variableList.add(0.2);
//        variableList.add(0.25);
        variableList.add(0.5);
        variableList.add(0.7);
        variableList.add(0.9);
        variableList.add(1.0);
        //variableList.add(10.0);


        String changingParameterName = null;
        for(double variable : variableList)
        {
            shrinkFactor = variable;
            //minCmdCntPerRtn = maxCmdCntPerRtn;
            changingParameterName = "shrinkFactor";



            changingParameterValue = variable;

            variableTrakcer.add(changingParameterValue); // add the variable name
            //List<Float> resultCollector = new ArrayList<>();

            if(!IS_RUNNIGN_BENCHMARK)
            {
                if(lastGeneratedZipfeanFor != zipF)
                {
                    lastGeneratedZipfeanFor = zipF;
                    String zipFianStr = prepareZipfian();
                    System.out.println(zipFianStr);
                    logStr += zipFianStr;
                }

                System.out.println("###################################");
                logStr += "###################################\n";

                System.out.println("IS_RUNNING_BENCHMARK = " + IS_RUNNING_BENCHMARK);
                logStr += "IS_RUNNING_BENCHMARK = " + IS_RUNNING_BENCHMARK + "\n";
                System.out.println("totalSampleCount = " + totalSampleCount + "\n");
                logStr += "totalSampleCount = " + totalSampleCount + "\n\n";


                System.out.println("shrinkFactor = " + shrinkFactor);
                logStr += "shrinkFactor = " + shrinkFactor + "\n";
                System.out.println("minCmdCntPerRtn = " + minCmdCntPerRtn);
                logStr += "minCmdCntPerRtn = " + minCmdCntPerRtn + "\n";
                System.out.println("maxCmdCntPerRtn = " + maxCmdCntPerRtn + "\n");
                logStr += "maxCmdCntPerRtn = " + maxCmdCntPerRtn + "\n\n";


                System.out.println("zipF = " + zipF);
                logStr += "zipF = " + zipF + "\n";
                System.out.println("devRegisteredOutOf65Dev = " + devRegisteredOutOf65Dev);
                logStr += "devRegisteredOutOf65Dev = " + devRegisteredOutOf65Dev + "\n";
                System.out.println("maxConcurrentRtn = " + maxConcurrentRtn + "\n");
                logStr += "maxConcurrentRtn = " + maxConcurrentRtn + "\n\n";


                System.out.println("longRrtnPcntg = " + longRrtnPcntg);
                logStr += "longRrtnPcntg = " + longRrtnPcntg + "\n";
                System.out.println("isAtleastOneLongRunning = " + isAtleastOneLongRunning);
                logStr += "isAtleastOneLongRunning = " + isAtleastOneLongRunning + "\n";
                System.out.println("minLngRnCmdTimSpn = " + minLngRnCmdTimSpn);
                logStr += "minLngRnCmdTimSpn = " + minLngRnCmdTimSpn + "\n";
                System.out.println("maxLngRnCmdTimSpn = " + maxLngRnCmdTimSpn + "\n");
                logStr += "maxLngRnCmdTimSpn = " + maxLngRnCmdTimSpn + "\n\n";

                System.out.println("minShrtCmdTimeSpn = " + minShrtCmdTimeSpn);
                logStr += "minShrtCmdTimeSpn = " + minShrtCmdTimeSpn + "\n";
                System.out.println("maxShrtCmdTimeSpn = " + maxShrtCmdTimeSpn + "\n");
                logStr += "maxShrtCmdTimeSpn = " + maxShrtCmdTimeSpn + "\n\n";


                System.out.println("devFailureRatio = " + devFailureRatio);
                logStr += "devFailureRatio = " + devFailureRatio + "\n";
                System.out.println("atleastOneDevFail = " + atleastOneDevFail);
                logStr += "atleastOneDevFail = " + atleastOneDevFail + "\n";
                System.out.println("mustCmdPercentage = " + mustCmdPercentage + "\n");
                logStr += "mustCmdPercentage = " + mustCmdPercentage + "\n\n";


                System.out.println("IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED);
                logStr += "IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED + "\n";
                System.out.println("IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED + "\n");
                logStr += "IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED + "\n\n";

                System.out.println("SIMULATION_START_TIME = " + SIMULATION_START_TIME);
                logStr += "SIMULATION_START_TIME = " + SIMULATION_START_TIME + "\n";
                System.out.println("MAX_DATAPOINT_COLLECTON_SIZE = " + MAX_DATAPOINT_COLLECTON_SIZE);
                logStr += "MAX_DATAPOINT_COLLECTON_SIZE = " + MAX_DATAPOINT_COLLECTON_SIZE + "\n";
                System.out.println("RANDOM_SEED = " + RANDOM_SEED);
                logStr += "RANDOM_SEED = " + RANDOM_SEED + "\n";
                System.out.println("MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = " + MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING + "\n");
                logStr += "MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = " + MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING + "\n\n";

                System.out.println("dataStorageDirectory = " + dataStorageDirectory + "\n");
                logStr += "dataStorageDirectory = " + dataStorageDirectory + "\n\n";

                System.out.println("###################################");
                logStr += "###################################\n";


//
//                System.out.println("Total Device Count: = " + devIDlist.size());
//                logStr += "Total Device Count: = " + devIDlist.size() + "\n";
//
//                System.out.println("Total Device Count: = " + devIDlist.size());
//                logStr += "Total Device Count: = " + devIDlist.size() + "\n";
//
//                System.out.println("totalConcurrentRtn = " + maxConcurrentRtn);
//                logStr += "totalConcurrentRtn = " + maxConcurrentRtn + "\n";
//
//                System.out.println("minCmdCntPerRtn = " + minCmdCntPerRtn);
//                logStr += "minCmdCntPerRtn = " + minCmdCntPerRtn + "\n";
//
//                System.out.println("maxCmdCntPerRtn = " + maxCmdCntPerRtn);
//                logStr += "maxCmdCntPerRtn = " + maxCmdCntPerRtn + "\n";
//
//                System.out.println("shrinkFactor = " + shrinkFactor );
//                logStr += "shrinkFactor = " + shrinkFactor + "\n";
//
//                System.out.println("zipF = " + zipF);
//                logStr += "zipF = " + zipF + "\n";
//
//                System.out.println("IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED);
//                logStr += "IS_PRE_LEASE_ALLOWED = " + IS_PRE_LEASE_ALLOWED + "\n";
//
//                System.out.println("IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED);
//                logStr += "IS_POST_LEASE_ALLOWED = " + IS_POST_LEASE_ALLOWED + "\n";
//
//                System.out.println("minShrtCmdTimeSpn = " + minShrtCmdTimeSpn);
//                logStr += "minShrtCmdTimeSpn = " + minShrtCmdTimeSpn + "\n";
//                System.out.println("maxShrtCmdTimeSpn = " + maxShrtCmdTimeSpn);
//                logStr += "maxShrtCmdTimeSpn = " + maxShrtCmdTimeSpn + "\n";
//
//                System.out.println("minLngRnCmdTimSpn = " + minLngRnCmdTimSpn);
//                logStr += "minLngRnCmdTimSpn = " + minLngRnCmdTimSpn + "\n";
//                System.out.println("maxLngRnCmdTimSpn = " + maxLngRnCmdTimSpn);
//                logStr += "maxLngRnCmdTimSpn = " + maxLngRnCmdTimSpn + "\n";
//
//                System.out.println("longRrtnPcntg = " + longRrtnPcntg);
//                logStr += "longRrtnPcntg = " + longRrtnPcntg + "\n";
//                System.out.println("isAtleastOneLongRunning = " + isAtleastOneLongRunning);
//                logStr += "isAtleastOneLongRunning = " + isAtleastOneLongRunning + "\n";
//
//                System.out.println("SIMULATION_START_TIME = " + SIMULATION_START_TIME);
//                logStr += "SIMULATION_START_TIME = " + SIMULATION_START_TIME + "\n";
//
//                System.out.println("totalSampleCount = " + totalSampleCount);
//                logStr += "totalSampleCount = " + totalSampleCount + "\n";
//
//                System.out.println("devFailureRatio = " + devFailureRatio);
//                logStr += "devFailureRatio = " + devFailureRatio + "\n";
//                System.out.println("atleastOneDevFail = " + atleastOneDevFail);
//                logStr += "atleastOneDevFail = " + atleastOneDevFail + "\n";
//
//                System.out.println("mustCmdPercentage = " + mustCmdPercentage);
//                logStr += "mustCmdPercentage = " + mustCmdPercentage + "\n";
//
//                System.out.println("RANDOM_SEED = " + RANDOM_SEED);
//                logStr += "RANDOM_SEED = " + RANDOM_SEED + "\n";
//
//                System.out.println("MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = " + MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING);
//                logStr += "MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = " + MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING + "\n";

//                System.out.println("###################################");
//                logStr += "###################################\n";
            }

            int resolution = 10;
            int stepSize = totalSampleCount / resolution;
            if(stepSize == 0)
                stepSize = 1;

            for(int I = 0 ; I < totalSampleCount ; I++)
            {
                List<Routine> routineSet = null;

                if(I == totalSampleCount - 1)
                {
                    if(IS_RUNNIGN_BENCHMARK)
                        System.out.println("currently Running BENCHMARK...... Progress = 100%");
                    else
                        System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + " 100%");
                }
                else if(totalSampleCount % stepSize == 0)
                {
                    if(IS_RUNNIGN_BENCHMARK)
                        System.out.println("currently Running BENCHMARK...... Progress = " + (int) (100.0 * ((float)I / (float)totalSampleCount)) + "%");
                    else
                        System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + (int) (100.0 * ((float)I / (float)totalSampleCount)) + "%");
                }

                if(IS_RUNNIGN_BENCHMARK)
                {
                    routineSet = benchmarkingTool.GetOneWorkload();
                }
                else
                {
                    routineSet = generateAutomatedRtn(RANDOM_SEED);
                }

                /*
                FailureResult failureResult;
                ExpResults expResult;
                float abtRatio;
                float recoverCmdRatio;
                */





                //for(MEASUREMENT_TYPE measurementType : measurementList)
                for(CONSISTENCY_TYPE consistency_type :  Temp.CONSISTENCY_ORDERING_LIST)
                {
                    ExpResults expResult = runExperiment(devIDlist, consistency_type, routineSet, SIMULATION_START_TIME);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.WAIT_TIME,
                            expResult.waitTimeHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.LATENCY_OVERHEAD,
                            expResult.latencyOverheadHistogram);

                    measurementCollector.collectData(changingParameterValue,consistency_type,
                            MEASUREMENT_TYPE.PARALLEL,
                            expResult.measurement.parallelismHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                            expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                            expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                            expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                            expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                            expResult.measurement.isvltn1_perRtnCollisionCountHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                            expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                            expResult.measurement.devUtilizationPrcntHistogram);

                    if(consistency_type == CONSISTENCY_TYPE.EVENTUAL)
                    {
                        measurementCollector.collectData(changingParameterValue, consistency_type,
                                MEASUREMENT_TYPE.STRETCH_RATIO,
                                expResult.stretchRatioHistogram);
                    }

                }







///////////////////////////////////////-------STRONG------////////////////////////////////////////////////////////////
                /*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.STRONG, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

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
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
                */
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////-------RELAXED STRONG-----/////////////////////////////////////////////////////////

                /*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.RELAXED_STRONG, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

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
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
                */
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////-------WEAK-----/////////////////////////////////////////////////////////
                /*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.WEAK, routineSet, SIMULATION_START_TIME);

                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);



                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
                */
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------EVENTUAL-----/////////////////////////////////////////////////////////

                /*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.EVENTUAL, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.STRETCH_RATIO, expResult.stretchRatioHistogram);

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
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
                */
///////////////////////////////////////////////////////////////////////////////////////////////////
/*
//////////////////////////////////////////-------LAZY-----/////////////////////////////////////////////////////////

                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it

 */
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------LAZY-FCFS-----/////////////////////////////////////////////////////////
/*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY_FCFS, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
*/

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////-------LAZY-PRIORITY-----/////////////////////////////////////////////////////////

                /*
                expResult = runExperiment(devIDlist, CONSISTENCY_TYPE.LAZY_PRIORITY, routineSet, SIMULATION_START_TIME);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.WAIT_TIME, expResult.waitTimeHistogram);
                measurementCollector.collectData(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.LATENCY_OVERHEAD, expResult.latencyOverheadHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.PARALLEL,
                        expResult.measurement.parallelismHistogram);

                ///////
                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                        expResult.measurement.isvltn5_routineLvlIsolationViolationTimePrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
                        expResult.measurement.isvltn4_cmdToCommitCollisionTimespanPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
                        expResult.measurement.isvltn3_CMDviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT,
                        expResult.measurement.isvltn2_RTNviolationPercentHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT,
                        expResult.measurement.isvltn1_perRtnCollisionCountHistogram);
                ///////

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDDER_MISMATCH,
                        expResult.measurement.orderingMismatchPrcntHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE,
                        expResult.measurement.orderingMismatchPrcntBUBBLEHistogram);

                measurementCollector.collectData(changingParameterValue,
                        CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.DEVICE_UTILIZATION,
                        expResult.measurement.devUtilizationPrcntHistogram);

                expResult = null; // ensures that the code below will not accidentally use it without reinitializing it
                */
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            }

            logStr += "\n=========================================================================\n";


            if(!globalDataCollector.containsKey(changingParameterValue))
                globalDataCollector.put(changingParameterValue, new LinkedHashMap<>());

            for(MEASUREMENT_TYPE measurementType : measurementList)
            {
                if(!globalDataCollector.get(changingParameterValue).containsKey(measurementType))
                    globalDataCollector.get(changingParameterValue).put(measurementType, new LinkedHashMap<>());

                if(measurementType == MEASUREMENT_TYPE.STRETCH_RATIO)
                {
                    double avg = measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, measurementType);
                    avg = (double)((int)(avg * 1000.0))/1000.0;
                    globalDataCollector.get(changingParameterValue).get(measurementType).put(CONSISTENCY_TYPE.EVENTUAL, avg);
                }
                else
                {
                    for(CONSISTENCY_TYPE consistency_type :  Temp.CONSISTENCY_ORDERING_LIST)
                    {
                        double avg = measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, consistency_type, measurementType);
                        avg = (double)((int)(avg * 1000.0))/1000.0;
                        globalDataCollector.get(changingParameterValue).get(measurementType).put(consistency_type, avg);
                    }
                }
            }


/*
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.WAIT_TIME));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.WAIT_TIME));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.WAIT_TIME));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.LATENCY_OVERHEAD));
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
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.PARALLEL));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.PARALLEL));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDDER_MISMATCH));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDDER_MISMATCH));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ORDERR_MISMATCH_BUBBLE));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.DEVICE_UTILIZATION));
            ////////////////////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN1_PER_RTN_COLLISION_COUNT));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN2_VIOLATED_RTN_PRCNT));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN));
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT));
            /////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.STRONG, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.RELAXED_STRONG, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.WEAK, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
//            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_FCFS, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            resultCollector.add(measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.LAZY_PRIORITY, MEASUREMENT_TYPE.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT));
            /////////////////////////////////////////////////////////////

            globalDataCollector.put(changingParameterValue, resultCollector);
            */
        }


        String globalResult = "\n--------------------------------\n";
        globalResult += "Summary-Start\t\n";

        Map<MEASUREMENT_TYPE, String> perMeasurementAvgMap = new LinkedHashMap<>();

        for(MEASUREMENT_TYPE measurementType : measurementList )
        {
            globalResult += "================================\n";
            globalResult += "MEASURING: " + measurementType.name() + "\n";

            String perMeasurementInfo = "";

            boolean isHeaderPrinted = false;
            for(double variable : variableList)
            {
                if(!isHeaderPrinted)
                {
                    perMeasurementInfo += changingParameterName + "\t";
                    for(CONSISTENCY_TYPE consistency_type : globalDataCollector.get(variable).get(measurementType).keySet())
                    {
                        perMeasurementInfo += CONSISTENCY_HEADER.get(consistency_type) + "\t";
                    }
                    perMeasurementInfo += "\n";

                    isHeaderPrinted = true;
                }

                perMeasurementInfo += variable + "\t";

                for(CONSISTENCY_TYPE consistency_type : globalDataCollector.get(changingParameterValue).get(measurementType).keySet())
                {
                    double avg = globalDataCollector.get(variable).get(measurementType).get(consistency_type);
                    perMeasurementInfo += avg + "\t";
                }

                perMeasurementInfo += "\n";
            }
            globalResult += perMeasurementInfo;
            globalResult += "================================\n";

            perMeasurementAvgMap.put(measurementType, perMeasurementInfo);
        }

        globalResult += "Summary-End\t\n";
        globalResult += "--------------------------------\n";



/*
        String globalResult = "\n--------------------------------\n";
        globalResult += "Summary-Start\t\n";
        //String header = "Variable\tStgAvg\tStgSD\tR.StgAvg\tR.StgSD\tLzyAvg\tLzySD\tEvnAvg\tEvnSD\tEvnStretchRatioAvg\tEvnStretchRatioSD";
        String header = "G0:Var";


        header += "\tG1:GSV_WaitTm\tG1:PSV_WaitTm\tG1:WV_WaitTm\tG1:EV_WaitTm\tG1:LzFCFS_WaitTm\tG1:LzPRIOTY_WaitTm";
        header += "\tG2:GSV_LtncyOvrhdPcnt\tG2:PSV_LtncyOvrhdPcnt\tG2:WV_LtncyOvrhdPcnt\tG2:EV_LtncyOvrhdPcnt\tG2:LzFCFS_LtncyOvrhdPcnt\tG2:LzPRIOTY_LtncyOvrhdPcnt";
        header += "\tG3:EV_Stretch";

        header += "\tG4:GSV_Abort\tG4:PSV_Abort\tG4:EV_Abort";
        header += "\tG5:GSV_RcvryCmdRatio\tG5:PSV_RcvryCmdRatio\tG5:EV_RcvryCmdRatio";

        header += "\tG6:GSV_Parrl\tG6:PSV_Parrl\tG6:WV_Parrl\tG6:EV_Parrl\tG6:LzFCFS_Parrl\tG6:LzPRIOTY_Parrl";
        header += "\tG7:GSV_OdrMismtch\tG7:PSV_OdrMismtch\tG7:WV_OdrMismtch\tG7:EV_OdrMismtch\tG7:LzFCFS_OdrMismtch\tG7:LzPRIOTY_OdrMismtch";
        header += "\tG71:GSV_OdrMismtchBBL\tG71:PSV_OdrMismtchBBL\tG71:WV_OdrMismtchBBL\tG71:EV_OdrMismtchBBL\tG71:LzFCFS_OdrMismtchBBL\tG71:LzPRIOTY_OdrMismtchBBL";
        header += "\tG8:GSV_DevUtlz\tG8:PSV_DevUtlz\tG8:WV_DevUtlz\tG8:EV_DevUtlz\tG8:LzFCFS_DevUtlz\tG8:LzPRIOTY_DevUtlz";

        header += "\tG9:GSV_Isvltn1PerRtnCollision\tG9:PSV_Isvltn1PerRtnCollision\tG9:WV_Isvltn1PerRtnCollision\tG9:EV_Isvltn1PerRtnCollision\tG9:LzFCFS_Isvltn1PerRtnCollision\tG9:LzPRIOTY_Isvltn1PerRtnCollision";
        header += "\tG10:GSV_Isvltn2ViolatedRtnPrcnt\tG10:PSV_Isvltn2ViolatedRtnPrcnt\tG10:WV_Isvltn2ViolatedRtnPrcnt\tG10:EV_Isvltn2ViolatedRtnPrcnt\tG10:LzFCFS_Isvltn2ViolatedRtnPrcnt\tG10:LzPRIOTY_Isvltn2ViolatedRtnPrcnt";
        header += "\tG11:GSV_Isvltn3ViolatedCmdPrcnt\tG11:PSV_Isvltn3ViolatedCmdPrcnt\tG11:WV_Isvltn3ViolatedCmdPrcnt\tG11:EV_Isvltn3ViolatedCmdPrcnt\tG11:LzFCFS_Isvltn3ViolatedCmdPrcnt\tG11:LzPRIOTY_Isvltn3ViolatedCmdPrcnt";
        header += "\tG12:GSV_Isvltn4CmdTimePcnt\tG12:PSV_Isvltn4CmdTimePcnt\tG12:WV_Isvltn4CmdTimePcnt\tG12:EV_Isvltn4CmdTimePcnt\tG12:LzFCFS_Isvltn4CmdTimePcnt\tG12:LzPRIOTY_Isvltn4CmdTimePcnt";
        header += "\tG13:GSV_Isvltn5RtnTimePcnt\tG13:PSV_Isvltn5RtnTimePcnt\tG13:WV_Isvltn5RtnTimePcnt\tG13:EV_Isvltn5RtnTimePcnt\tG13:LzFCFS_Isvltn5RtnTimePcnt\tG13:LzPRIOTY_Isvltn5RtnTimePcnt";
        */

        /*
        header += "\tG1:GSV_WaitTm\tG1:PSV_WaitTm\tG1:WV_WaitTm\tG1:EV_WaitTm\tG1:LV_WaitTm\tG1:FCFSV_WaitTm\tG1:LzPRIOTY_WaitTm";
        header += "\tG2:GSV_LtncyOvrhdPcnt\tG2:PSV_LtncyOvrhdPcnt\tG2:WV_LtncyOvrhdPcnt\tG2:EV_LtncyOvrhdPcnt\tG2:LV_LtncyOvrhdPcnt\tG2:FCFSV_LtncyOvrhdPcnt\tG2:LzPRIOTY_LtncyOvrhdPcnt";
        header += "\tG3:EV_Stretch";

        header += "\tG4:GSV_Abort\tG4:PSV_Abort\tG4:EV_Abort";
        header += "\tG5:GSV_RcvryCmdRatio\tG5:PSV_RcvryCmdRatio\tG5:EV_RcvryCmdRatio";

        header += "\tG6:GSV_Parrl\tG6:PSV_Parrl\tG6:WV_Parrl\tG6:EV_Parrl\tG6:LV_Parrl\tG6:FCFSV_Parrl\tG6:LzPRIOTY_Parrl";
        header += "\tG7:GSV_OdrMismtch\tG7:PSV_OdrMismtch\tG7:WV_OdrMismtch\tG7:EV_OdrMismtch\tG7:LV_OdrMismtch\tG7:FCFSV_OdrMismtch\tG7:LzPRIOTY_OdrMismtch";
        header += "\tG8:GSV_DevUtlz\tG8:PSV_DevUtlz\tG8:WV_DevUtlz\tG8:EV_DevUtlz\tG8:LV_DevUtlz\tG8:FCFSV_DevUtlz\tG8:LzPRIOTY_DevUtlz";

        header += "\tG9:GSV_IsvltnPerRtnCollision\tG9:PSV_IsvltnPerRtnCollision\tG9:WV_IsvltnPerRtnCollision\tG9:EV_IsvltnPerRtnCollision\tG9:LV_IsvltnPerRtnCollision\tG9:FCFSV_IsvltnPerRtnCollision\tG9:LzPRIOTY_IsvltnPerRtnCollision";
        header += "\tG10:GSV_IsvltnViolatedRtnPrcnt\tG10:PSV_IsvltnViolatedRtnPrcnt\tG10:WV_IsvltnViolatedRtnPrcnt\tG10:EV_IsvltnViolatedRtnPrcnt\tG10:LV_IsvltnViolatedRtnPrcnt\tG10:FCFSV_IsvltnViolatedRtnPrcnt\tG10:LzPRIOTY_IsvltnViolatedRtnPrcnt";
        header += "\tG11:GSV_IsvltnViolatedCmdPrcnt\tG11:PSV_IsvltnViolatedCmdPrcnt\tG11:WV_IsvltnViolatedCmdPrcnt\tG11:EV_IsvltnViolatedCmdPrcnt\tG11:LV_IsvltnViolatedCmdPrcnt\tG11:FCFSV_IsvltnViolatedCmdPrcnt\tG11:LzPRIOTY_IsvltnViolatedCmdPrcnt";
        header += "\tG12:GSV_IsvltnCmdToCommitVioltnTimePcnt\tG12:PSV_IsvltnCmdToCommitVioltnTimePcnt\tG12:WV_IsvltnCmdToCommitVioltnTimePcnt\tG12:EV_IsvltnCmdToCommitVioltnTimePcnt\tG12:LV_IsvltnCmdToCommitVioltnTimePcnt\tG12:FCFSV_IsvltnCmdToCommitVioltnTimePcnt\tG12:LzPRIOTY_IsvltnCmdToCommitVioltnTimePcnt";
*/
        /*
        header += "\t"; // NOTE: this tab is required for the python separator
        globalResult += header + "\n";
        for(double variable : variableTrakcer)
        {
            globalResult += variable + "\t";

            for(float stats : globalDataCollector.get(variable))
            {
                String formattedNumber = String.format("%.3f", stats);
                globalResult += stats + "\t";
            }

            globalResult += "\n";
        }
        globalResult += "Summary-End\t\n";
        globalResult += "--------------------------------\n";
*/
        logStr += globalResult;


        ////////////////////-CREATING-SUBDIRECTORY-/////////////////////////////
        if(changingParameterName == null)
        {
            System.out.println("\n\n ERROR: changingParameterName was not initialized! something is wrong. Terminating...\n\n");
            System.exit(1);
        }

        String epoch = System.currentTimeMillis() + "";
        String parentDirPath = dataStorageDirectory + File.separator + epoch + "_VARY_"+ changingParameterName;
        parentDirPath += "_R_" + maxConcurrentRtn + "_C_" + minCmdCntPerRtn + "-" + maxCmdCntPerRtn;

        File parentDir = new File(parentDirPath);
        if(!parentDir.exists())
        {
            parentDir.mkdir();
        }

        String avgMeasurementDirectoryPath = parentDirPath + File.separator + "avg";
        File avgDir = new File(avgMeasurementDirectoryPath);
        if(!avgDir.exists())
        {
            avgDir.mkdir();
        }

        ////////////////////////////////////////////////////////////////

        try
        {
            //String fileName = "VARY_" + changingParameterName + ".dat";
            String fileName = "Overall" + changingParameterName + ".txt";
            String filePath = parentDirPath + File.separator + fileName;

            Writer fileWriter = new FileWriter(filePath);
            fileWriter.write(logStr);
            fileWriter.close();

            for(Map.Entry<MEASUREMENT_TYPE, String> entry : perMeasurementAvgMap.entrySet())
            {
                String measurementFilePath = avgMeasurementDirectoryPath + File.separator + entry.getKey().name() + ".dat";

                fileWriter = new FileWriter(measurementFilePath);
                fileWriter.write(entry.getValue());
                fileWriter.close();
            }
        }
        catch (IOException e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }

        System.out.println("\n\nPROCESSING.....");
        measurementCollector.writeStatsInFile(parentDirPath, changingParameterName);
        System.out.println(globalResult);


    }

    private static DEV_ID getZipfDistDevID(float randDouble)
    {
        assert(0 < devID_ProbBoundaryMap.size());
        assert(0.0f <= randDouble && randDouble <= 1.0f);

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

        ZipfDistribution zipf = new ZipfDistribution(numberOfElements, zipF);

        List<Float> cumulativeProbabilityList = new ArrayList<>();

        for(int I = 0 ; I < devIDlist.size() ; I++)
        {
            float probability = (float)zipf.probability(I + 1);

            if(I == 0)
                cumulativeProbabilityList.add(probability);
            else
                cumulativeProbabilityList.add(probability + cumulativeProbabilityList.get(I - 1));
        }

        //System.out.println(cumulativeProbabilityList);

        float lowerInclusive = 0.0f;

        for(int I = 0 ; I < devIDlist.size() ; I++)
        {
            float upperExclusive = cumulativeProbabilityList.get(I);

            if(I == devIDlist.size() - 1)
                upperExclusive = 1.01f;

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
            DEV_ID devId = getZipfDistDevID(rand.nextFloat());
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

        return str;
    }


    private static int ROUTINE_ID = 0;

    public static int getUniqueRtnID()
    {
        return Temp.ROUTINE_ID++;
    }

    private static List<Routine> generateAutomatedRtn(int nonNegativeSeed)
    {
        if(maxCmdCntPerRtn < minCmdCntPerRtn ||
                maxLngRnCmdTimSpn < minLngRnCmdTimSpn ||
                maxShrtCmdTimeSpn < minShrtCmdTimeSpn
        )
        {
            System.out.println("\n ERROR: maxCmdCntPerRtn = " + maxCmdCntPerRtn + ", minCmdCntPerRtn = " + minCmdCntPerRtn);
            System.out.println("\n ERROR: maxLngRnCmdTimSpn = " + maxLngRnCmdTimSpn + ", minLngRnCmdTimSpn = " + minLngRnCmdTimSpn);
            System.out.println("\n ERROR: maxShrtCmdTimeSpn = " + maxShrtCmdTimeSpn + ", minShrtCmdTimeSpn = " + minShrtCmdTimeSpn + "\n Terminating.....");
            System.exit(1);
        }

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
            float nextDbl = rand.nextFloat();
            nextDbl = (nextDbl == 1.0f) ? nextDbl - 0.001f : nextDbl;
            boolean isLongRunning = (nextDbl < longRrtnPcntg);

            if(isLongRunning)
                longRunningRoutineCount++;

            if(isAtleastOneLongRunning && (RoutineCount == totalConcurrentRtn - 1) && longRunningRoutineCount == 0)
            {
                isLongRunning = true; // at least one routine will be long running;
            }


            int difference = 1 + (int)maxCmdCntPerRtn - (int)minCmdCntPerRtn;
            int totalCommandInThisRtn = (int)minCmdCntPerRtn + rand.nextInt(difference);


            if(devIDlist.size() < totalCommandInThisRtn )
            {
                System.out.println("\n ERROR: ID 2z3A9s : totalCommandInThisRtn = " + totalCommandInThisRtn + " > devIDlist.size() = " + devIDlist.size());
                System.exit(1);
            }

            Map<DEV_ID, Integer> devIDDurationMap = new HashMap<>();
            List<DEV_ID> devList = new ArrayList<>();

            while(devIDDurationMap.size() < totalCommandInThisRtn)
            {
                DEV_ID devID;

                devID = getZipfDistDevID(rand.nextFloat());

                if(devIDDurationMap.containsKey(devID))
                    continue;

                int duration;
                int currentDurationMapSize = devIDDurationMap.size();
                int middleCommandIndex = totalCommandInThisRtn / 2;
                if(isLongRunning && ( currentDurationMapSize == middleCommandIndex) )
                { // select the  middle command as long running command
                    difference = 1 + (int)maxLngRnCmdTimSpn - (int)minLngRnCmdTimSpn;
                    duration = (int)minLngRnCmdTimSpn + rand.nextInt(difference);
                }
                else
                {
                    difference = 1 + (int)maxShrtCmdTimeSpn - (int)minShrtCmdTimeSpn;
                    duration = (int)minShrtCmdTimeSpn + rand.nextInt(difference);
                }

                devIDDurationMap.put(devID, duration);
                devList.add(devID);
            }

            Routine rtn = new Routine();

            for(DEV_ID devID : devList)
            {
                assert(devIDDurationMap.containsKey(devID));

                nextDbl = rand.nextFloat();
                nextDbl = (nextDbl == 1.0f) ? nextDbl - 0.001f : nextDbl;
                boolean isMust = (nextDbl < mustCmdPercentage);
                Command cmd = new Command(devID, devIDDurationMap.get(devID), isMust);
                rtn.addCommand(cmd);
            }
            routineList.add(rtn);
        }

        Collections.shuffle(routineList, rand);

        if(shrinkFactor == 0.0)
        {
            for(int index = 0 ; index < routineList.size() ; ++index)
            {
                routineList.get(index).registrationTime = SIMULATION_START_TIME;
            }
        }
        else
        {
            float allRtnBackToBackExcTime = 0.0f;
            for(Routine rtn : routineList)
            {
                allRtnBackToBackExcTime += rtn.getBackToBackCmdExecutionTimeWithoutGap();
            }

            double simulationLastRtnStartTime = allRtnBackToBackExcTime * shrinkFactor;

            int upperLimit = (int)Math.ceil(simulationLastRtnStartTime);

            List<Integer> randStartPointList = new ArrayList<>();
            for(int I = 0 ; I < routineList.size() ; I++)
            {
                int randStartPoint = SIMULATION_START_TIME +  ((upperLimit == 0) ? 0 : rand.nextInt(upperLimit));
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

        expResults.measurement = new Measurement(lockTable);


        for(Routine routine : perExpRtnList)
        {
            float data;
            Integer count;

            //////////////////////////////////////////////////
            //expResults.waitTimeList.add(routine.getStartDelay());
            data = routine.getStartDelay();
            count = expResults.waitTimeHistogram.get(data);

            if(count == null)
                expResults.waitTimeHistogram.put(data, 1);
            else
                expResults.waitTimeHistogram.put(data, count + 1);
            //////////////////////////////////////////////////
            //expResults.latencyOverheadList.add(routine.getLatencyOverheadPrcnt());
            data = routine.getLatencyOverheadPrcnt();
            count = expResults.latencyOverheadHistogram.get(data);

            if(count == null)
                expResults.latencyOverheadHistogram.put(data, 1);
            else
                expResults.latencyOverheadHistogram.put(data, count + 1);
            //////////////////////////////////////////////////
            //expResults.stretchRatioList.add(routine.getStretchRatio());
            data = routine.getStretchRatio();
            count = expResults.stretchRatioHistogram.get(data);

            if(count == null)
                expResults.stretchRatioHistogram.put(data, 1);
            else
                expResults.stretchRatioHistogram.put(data, count + 1);
            //////////////////////////////////////////////////

            assert(!expResults.waitTimeHistogram.isEmpty());
            assert(!expResults.latencyOverheadHistogram.isEmpty());
            assert(!expResults.stretchRatioHistogram.isEmpty());
        }

        return expResults;
    }
}
