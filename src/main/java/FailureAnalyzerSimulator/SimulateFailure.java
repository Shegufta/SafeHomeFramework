package FailureAnalyzerSimulator;

import BenchmarkingTool.*;
import SafeHomeSimulator.*;
import java.util.*;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 10-Oct-19
 * @time 6:43 PM
 */

// graph plotting tool command:  "python .\gen_all.py -d C:\Users\shegufta\Desktop\smartHomeData\1568337471715_VARY_maxConcurrentRtn_R_101_C_6"

public class SimulateFailure
{
    public static final boolean IS_RUNNING_BENCHMARK = SysParamSngltn.getInstance().IS_RUNNING_BENCHMARK; //false; // Careful... if it is TRUE, all other parameters will be in don't care mode!
    private static final int totalSampleCount = SysParamSngltn.getInstance().totalSampleCount; //1000;//7500;//10000; // 100000;

    public static final boolean isVaryShrinkFactor = SysParamSngltn.getInstance().isVaryShrinkFactor;
    public static final boolean isVaryCommandCntPerRtn = SysParamSngltn.getInstance().isVaryCommandCntPerRtn;
    public static final boolean isVaryZipfAlpha = SysParamSngltn.getInstance().isVaryZipfAlpha;
    public static final boolean isVaryLongRunningPercent = SysParamSngltn.getInstance().isVaryLongRunningPercent;
    public static final boolean isVaryLongRunningDuration = SysParamSngltn.getInstance().isVaryLongRunningDuration;
    public static final boolean isVaryShortRunningDuration = SysParamSngltn.getInstance().isVaryShortRunningDuration;
    public static final boolean isVaryMustCmdPercentage = SysParamSngltn.getInstance().isVaryMustCmdPercentage;
    public static final boolean isVaryDevFailureRatio = SysParamSngltn.getInstance().isVaryDevFailureRatio;

    public static final String commaSeprtdVarListString = SysParamSngltn.getInstance().commaSeprtdVarListString;
    public static List<Double> variableList = SysParamSngltn.getInstance().variableList;

    public static final String commaSeprtdCorrespondingUpperBoundListString =  SysParamSngltn.getInstance().commaSeprtdCorrespondingUpperBoundListString;
    public static final List<Double> variableCorrespndinMaxValList = SysParamSngltn.getInstance().variableCorrespndinMaxValList;

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

    private static double minShrtCmdTimeSpn = SysParamSngltn.getInstance().minShrtCmdTimeSpn; //  10;
    private static double maxShrtCmdTimeSpn = SysParamSngltn.getInstance().maxShrtCmdTimeSpn; //  minShrtCmdTimeSpn * 6;

    private static double devFailureRatio = SysParamSngltn.getInstance().devFailureRatio; //  0.0;
    private static final boolean atleastOneDevFail = SysParamSngltn.getInstance().atleastOneDevFail; //  false;
    private static double mustCmdPercentage = SysParamSngltn.getInstance().mustCmdPercentage; //  1.0;
    private static int failureAnalyzerSampleCount = SysParamSngltn.getInstance().failureAnalyzerSampleCount;

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
    public static List<CONSISTENCY_TYPE> CONSISTENCY_ORDERING_LIST = new ArrayList<>();
    ///////////////////////////////////////////////////////////////////////////////////


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


    private static String preparePrintableParameters()
    {
        String logStr = "";

        System.out.println("###################################");
        logStr += "###################################\n";

        System.out.println("IS_RUNNING_BENCHMARK = " + IS_RUNNING_BENCHMARK);
        logStr += "IS_RUNNING_BENCHMARK = " + IS_RUNNING_BENCHMARK + "\n";
        System.out.println("totalSampleCount = " + totalSampleCount + "\n");
        logStr += "totalSampleCount = " + totalSampleCount + "\n\n";


        System.out.println("isVaryShrinkFactor = " + isVaryShrinkFactor);
        logStr += "isVaryShrinkFactor = " + isVaryShrinkFactor + "\n";
        System.out.println("isVaryCommandCntPerRtn = " + isVaryCommandCntPerRtn);
        logStr += "isVaryCommandCntPerRtn = " + isVaryCommandCntPerRtn + "\n";
        System.out.println("isVaryZipfAlpha = " + isVaryZipfAlpha);
        logStr += "isVaryZipfAlpha = " + isVaryZipfAlpha + "\n";
        System.out.println("isVaryLongRunningPercent = " + isVaryLongRunningPercent);
        logStr += "isVaryLongRunningPercent = " + isVaryLongRunningPercent + "\n";
        System.out.println("isVaryLongRunningDuration = " + isVaryLongRunningDuration);
        logStr += "isVaryLongRunningDuration = " + isVaryLongRunningDuration + "\n";
        System.out.println("isVaryShortRunningDuration = " + isVaryShortRunningDuration);
        logStr += "isVaryShortRunningDuration = " + isVaryShortRunningDuration + "\n";
        System.out.println("isVaryMustCmdPercentage = " + isVaryMustCmdPercentage);
        logStr += "isVaryMustCmdPercentage = " + isVaryMustCmdPercentage + "\n";
        System.out.println("isVaryDevFailureRatio = " + isVaryDevFailureRatio + "\n");
        logStr += "isVaryDevFailureRatio = " + isVaryDevFailureRatio + "\n\n";


        System.out.println("commaSeprtdVarListString = " + commaSeprtdVarListString);
        logStr += "commaSeprtdVarListString = " + commaSeprtdVarListString + "\n";
        System.out.println("commaSeprtdCorrespondingUpperBoundListString = " + commaSeprtdCorrespondingUpperBoundListString + "\n");
        logStr += "commaSeprtdCorrespondingUpperBoundListString = " + commaSeprtdCorrespondingUpperBoundListString + "\n\n";


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
        System.out.println("mustCmdPercentage = " + mustCmdPercentage);
        logStr += "mustCmdPercentage = " + mustCmdPercentage + "\n";
        System.out.println("failureAnalyzerSampleCount = " + failureAnalyzerSampleCount + "\n");
        logStr += "failureAnalyzerSampleCount = " + failureAnalyzerSampleCount + "\n\n";


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

        return logStr;
    }

    public static void main (String[] args) throws Exception
    {
        Benchmark benchmarkingTool = null;

        if(IS_RUNNING_BENCHMARK)
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

        //List<Double> variableTrakcer = new ArrayList<>();
        Double changingParameterValue = -1.0;
        double lastGeneratedZipfeanFor = Double.MAX_VALUE; // NOTE: declare zipfean here... DO NOT declare it inside the for loop!

        ////////////////////////////////////////////////////////////////////////////////
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.SUPER_STRONG);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.STRONG);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.RELAXED_STRONG);
        CONSISTENCY_ORDERING_LIST.add(CONSISTENCY_TYPE.EVENTUAL);
        ////////////////////////////////////////////////////////////////////////////////
        List<MEASUREMENT_TYPE> measurementList = new ArrayList<>();
        measurementList.add(MEASUREMENT_TYPE.ABORT_RATE);
        measurementList.add(MEASUREMENT_TYPE.RECOVERY_CMD_TOTAL);
        measurementList.add(MEASUREMENT_TYPE.RECOVERY_CMD_PER_RTN);

        if(SysParamSngltn.getInstance().isMeasureEVroutineInsertionTime)
            measurementList.add(MEASUREMENT_TYPE.EV_ROUTINE_INSERT_TIME_MICRO_SEC);
        //measurementList.add(MEASUREMENT_TYPE.EXECUTION_LATENCY_MS);
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        boolean isBenchmarkingDoneForSinglePass = false;

        String changingParameterName = null;
        for(int varIdx = 0; varIdx < variableList.size() || IS_RUNNING_BENCHMARK; varIdx++)
        {
            if(IS_RUNNING_BENCHMARK && isBenchmarkingDoneForSinglePass)
                break;

            if(IS_RUNNING_BENCHMARK)
            {
                variableList = new ArrayList<>();
                variableList.add(-123.0);

                isBenchmarkingDoneForSinglePass = true;
                changingParameterValue = variableList.get(0);
                changingParameterName = "benchmarking";
            }
            else
            {
                changingParameterValue = variableList.get(varIdx);

                if(isVaryShrinkFactor)
                {
                    shrinkFactor = changingParameterValue;
                    changingParameterName = "shrinkFactor";
                }
                else if(isVaryZipfAlpha)
                {
                    zipF = changingParameterValue;
                    changingParameterName = "zipF";
                }
                else if(isVaryLongRunningPercent)
                {
                    longRrtnPcntg = changingParameterValue;
                    changingParameterName = "longRrtnPcntg";
                }
                else if(isVaryCommandCntPerRtn)
                {
                    double maxVal = variableCorrespndinMaxValList.get(varIdx);

                    minCmdCntPerRtn = changingParameterValue;
                    maxCmdCntPerRtn = maxVal;
                    changingParameterName = "minCmdCntPerRtn";
                }
                else if(isVaryLongRunningDuration)
                {
                    double maxVal = variableCorrespndinMaxValList.get(varIdx);

                    minLngRnCmdTimSpn = changingParameterValue;
                    maxLngRnCmdTimSpn = maxVal;
                    changingParameterName = "minLngRnCmdTimSpn";
                }
                else if(isVaryShortRunningDuration)
                {
                    double maxVal = variableCorrespndinMaxValList.get(varIdx);

                    minShrtCmdTimeSpn = changingParameterValue;
                    maxShrtCmdTimeSpn = maxVal;
                    changingParameterName = "minShrtCmdTimeSpn";
                }
                else if(isVaryMustCmdPercentage)
                {
                    mustCmdPercentage = changingParameterValue;
                    changingParameterName = "mustPrcnt";
                }
                else if(isVaryDevFailureRatio)
                {
                    devFailureRatio = changingParameterValue;
                    changingParameterName = "DevFailPrcnt";
                }
                else
                {
                    System.out.println("Error: unknown selection.... Terminating...");
                    System.exit(1);
                }

                if(lastGeneratedZipfeanFor != zipF)
                {
                    lastGeneratedZipfeanFor = zipF;
                    String zipFianStr = prepareZipfian();
                    System.out.println(zipFianStr);
                    logStr += zipFianStr;
                }

                logStr += preparePrintableParameters();
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
                    if(IS_RUNNING_BENCHMARK)
                        System.out.println("currently Running BENCHMARK...... Progress = 100%");
                    else
                        System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + " 100%");
                }
                else if(totalSampleCount % stepSize == 0)
                {
                    if(IS_RUNNING_BENCHMARK)
                        System.out.println("currently Running BENCHMARK...... Progress = " + (int) (100.0 * ((float)I / (float)totalSampleCount)) + "%");
                    else
                        System.out.println("currently Running for, " + changingParameterName + " = " +  changingParameterValue  + " Progress = " + (int) (100.0 * ((float)I / (float)totalSampleCount)) + "%");
                }

                if(IS_RUNNING_BENCHMARK)
                {
                    routineSet = benchmarkingTool.GetOneWorkload();
                }
                else
                {
                    routineSet = generateAutomatedRtn(RANDOM_SEED);
                }


                for(CONSISTENCY_TYPE consistency_type :  CONSISTENCY_ORDERING_LIST)
                {
                    ExpResults expResult = runExperiment(devIDlist, consistency_type, routineSet, SIMULATION_START_TIME);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.ABORT_RATE,
                            expResult.abortHistogram);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.RECOVERY_CMD_TOTAL,
                            expResult.totalRollbackHistogramGSVPSV);

                    measurementCollector.collectData(changingParameterValue, consistency_type,
                            MEASUREMENT_TYPE.RECOVERY_CMD_PER_RTN,
                            expResult.perRtnRollbackCmdHistogram);

                    if(expResult.EV_executionLatencyHistogram != null)
                    {
                        measurementCollector.collectData(changingParameterValue, consistency_type,
                                MEASUREMENT_TYPE.EV_ROUTINE_INSERT_TIME_MICRO_SEC,
                                expResult.EV_executionLatencyHistogram);
                    }

//                    measurementCollector.collectData(changingParameterValue, consistency_type,
//                            MEASUREMENT_TYPE.EXECUTION_LATENCY_MS,
//                            expResult.executionLatencyHistogram);

                }
            }

            logStr += "\n=========================================================================\n";


            if(!globalDataCollector.containsKey(changingParameterValue))
                globalDataCollector.put(changingParameterValue, new LinkedHashMap<>());

            for(MEASUREMENT_TYPE measurementType : measurementList)
            {
                if(!globalDataCollector.get(changingParameterValue).containsKey(measurementType))
                    globalDataCollector.get(changingParameterValue).put(measurementType, new LinkedHashMap<>());

                if(measurementType == MEASUREMENT_TYPE.STRETCH_RATIO )
                {
                    if(CONSISTENCY_ORDERING_LIST.contains(CONSISTENCY_TYPE.EVENTUAL))
                    {
                        double avg = measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, measurementType);
                        avg = (double)((int)(avg * 1000.0))/1000.0;
                        globalDataCollector.get(changingParameterValue).get(measurementType).put(CONSISTENCY_TYPE.EVENTUAL, avg);
                    }
                }
                else if(measurementType == MEASUREMENT_TYPE.EV_ROUTINE_INSERT_TIME_MICRO_SEC)
                {
                    if(CONSISTENCY_ORDERING_LIST.contains(CONSISTENCY_TYPE.EVENTUAL))
                    {
                        double avg = measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, CONSISTENCY_TYPE.EVENTUAL, measurementType);
                        avg = (double)((int)(avg * 1000.0))/1000.0;
                        globalDataCollector.get(changingParameterValue).get(measurementType).put(CONSISTENCY_TYPE.EVENTUAL, avg);
                    }
                }
                else
                {
                    for(CONSISTENCY_TYPE consistency_type :  CONSISTENCY_ORDERING_LIST)
                    {
                        double avg = measurementCollector.finalizePrepareStatsAndGetAvg(changingParameterValue, consistency_type, measurementType);
                        avg = (double)((int)(avg * 1000.0))/1000.0;
                        globalDataCollector.get(changingParameterValue).get(measurementType).put(consistency_type, avg);
                    }
                }
            }
        }


        String globalResult = "\n--------------------------------\n";
        globalResult += "Summary-Start\t\n";

        Map<MEASUREMENT_TYPE, String> perMeasurementAvgMap = new LinkedHashMap<>();

        for(MEASUREMENT_TYPE measurementType : measurementList )
        {
            if(measurementType == MEASUREMENT_TYPE.STRETCH_RATIO && !CONSISTENCY_ORDERING_LIST.contains(CONSISTENCY_TYPE.EVENTUAL))
                continue;

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
                        perMeasurementInfo += HeaderListSnglTn.getInstance().CONSISTENCY_HEADER.get(consistency_type) + "\t";
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
        measurementCollector.writeStatsInFile(parentDirPath, changingParameterName,
                HeaderListSnglTn.getInstance().CONSISTENCY_HEADER,
                CONSISTENCY_ORDERING_LIST);
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
        return ROUTINE_ID++;
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


        Map<Float, Integer> EV_executionLatencyHistogram = lockTable.register(perExpRtnList, _simulationStartTime);

        ExpResults expResults = new ExpResults();

        FailureAnalyzer failurleAnalyzer = new FailureAnalyzer(lockTable.lockTable, _consistencyType);

        failurleAnalyzer.simulateFailure(devFailureRatio,
                atleastOneDevFail,
                RANDOM_SEED,
                failureAnalyzerSampleCount,
                expResults
                );

        if(_consistencyType == CONSISTENCY_TYPE.EVENTUAL && SysParamSngltn.getInstance().isMeasureEVroutineInsertionTime)
            expResults.EV_executionLatencyHistogram = EV_executionLatencyHistogram;
        else
            expResults.EV_executionLatencyHistogram = null;


        return expResults;
    }
}

