package Temp;


import org.apache.commons.math3.distribution.ZipfDistribution;

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
    private static int maxCommandPerRtn = 5; // in current version totalCommandInThisRtn = maxCommandPerRtn;
    private static int maxConcurrentRtn = 8; //in current version totalConcurrentRtn = maxConcurrentRtn;
    private static double zipfCoefficient = 0.01;
    private static double longRunningRtnPercentage = 0.0;
    private static final boolean atleastOneLongRunning = true;

    private static double devFailureRatio = 0.0;
    private static final boolean atleastOneDevFail = false;
    private static double mustCmdPercentage = 1.0;

    private static int longRunningCmdDuration = 100;
    private static final int shortCmdDuration = 1;

    private static final int totalSampleCount = 100000;
    private static final boolean isPrint = false;

    private static List<DEV_ID> devIDlist = new ArrayList<>();
    private static Map<DEV_ID, ZipfProbBoundary> devID_ProbBoundaryMap = new HashMap<>();

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


        System.out.println(str);

        return str;
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
                    duration = longRunningCmdDuration;
                }
                else
                {
                    duration = shortCmdDuration;
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

    /*
    private static List<Routine> generateRoutine(int nonNegativeSeed)
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

        //int totalConcurrentRtn = 1 + rand.nextInt(maxConcurrentRtn);
        int totalConcurrentRtn = maxConcurrentRtn;

        for(int RoutineCount = 0 ; RoutineCount < totalConcurrentRtn ; ++RoutineCount)
        {
            //int totalCommandInThisRtn = 1 + rand.nextInt(maxCommandPerRtn);
            int totalCommandInThisRtn = maxCommandPerRtn;
            assert(totalCommandInThisRtn <= devIDlist.size());

            boolean longRunningSelected = false;
            Map<DEV_ID, Integer> devIDDurationMap = new HashMap<>();

            while(devIDDurationMap.size() < totalCommandInThisRtn)
            {
                DEV_ID randDev = devIDlist.get( rand.nextInt(devIDlist.size()) );

                if(devIDDurationMap.containsKey(randDev))
                    continue;

                int duration = shortCmdDuration;

                if(!longRunningSelected)
                {
                    if( rand.nextDouble() < longRunningProbability)
                    {
                        longRunningSelected = true;
                        duration = longRunningCmdDuration;
                    }
                }

                devIDDurationMap.put(randDev, duration);
            }

            Routine rtn = new Routine();

            for(Map.Entry<DEV_ID, Integer> entry : devIDDurationMap.entrySet())
            {
                Command cmd = new Command(entry.getKey(), entry.getValue());
                rtn.addCommand(cmd);
            }

            routineList.add(rtn);

        }

        return routineList;
    }
    */

    public static ExpResults getStats(String itemName, List<Double> list)
    {
        double itemCount = list.size();

        double sum = 0.0;

        for(double item : list)
        {
            sum += item;
        }

        double avg = 0.0;

        if(0 < itemCount)
        {
            avg = sum / itemCount;
        }

        sum = 0.0;
        for(double item : list)
        {
            sum += (item - avg)*(item - avg);
        }

        if(0 < itemCount)
        {
            sum = sum / itemCount;
        }

        double standardDeviation = Math.sqrt(sum);

        ExpResults expResults = new ExpResults();
        expResults.itemCount = itemCount;
        expResults.rawAvg = avg;
        expResults.roundedAvg = (int) Math.round(avg);
        expResults.rawSD = standardDeviation;
        expResults.roundedSD = (int) Math.round(standardDeviation);

        expResults.logString = String.format( "%20s",itemName);
        expResults.logString += ": count = " + String.format("%.0f",itemCount);
        expResults.logString += "; avg = " + String.format("%7.2f",avg);
        expResults.logString += "; sd = " + String.format("%7.2f",standardDeviation);

        return expResults;
    }

    public static ExpResults runExperiment(List<DEV_ID> _devIDlist, CONSISTENCY_TYPE _consistencyType, List<Routine> _rtnList)
    {

        if(isPrint) System.out.println("\n------------------------------------------------------------------" );
        String logString = "\n------------------------------------------------------------------\n";

        if(isPrint) System.out.println("---------------------- " + _consistencyType.name() + " ------------------------------");
        logString += "---------------------- " + _consistencyType.name() + " ------------------------------\n";

        LockTable lockTable = new LockTable(_devIDlist, _consistencyType);
        List<Routine> rtnList = new ArrayList<>(_rtnList);

        int timeTick = 0;
        lockTable.register(rtnList, timeTick);

        if(isPrint) System.out.println(lockTable.toString());
        logString += lockTable.toString() + "\n";

        if(isPrint) System.out.println("----------------------");
        logString += "----------------------\n";

        ExpResults expResults = new ExpResults();
        expResults.failureAnalyzer = new FailureAnalyzer(lockTable.lockTable, _consistencyType);


        for(Routine routine : rtnList)
        {
            if(isPrint) System.out.println(routine);
            logString += routine + "\n";
            expResults.delayList.add(routine.getStartDelay());
            expResults.stretchRatioList.add(routine.getStretchRatio());
        }

        if(isPrint) System.out.println("----------------------");
        logString += "----------------------\n";

        if(isPrint) System.out.println(getStats("DELAY", expResults.delayList).logString);
        logString += getStats("DELAY", expResults.delayList) + "\n";

        if(isPrint) System.out.println(getStats("STRETCH_RATIO", expResults.stretchRatioList).logString);
        logString += getStats("STRETCH_RATIO", expResults.stretchRatioList) + "\n";

        expResults.logString = logString;

        return expResults;

        /*
        LockTable lockTable = new LockTable(devIDlist, CONSISTENCY_TYPE.EVENTUAL);
        List<Routine> rtnList = new ArrayList<>();

        for(int timeTick = 0 ; timeTick <= simulationTimeUnit ; ++timeTick)
        {
            if(0 == timeTick % rutineBurstInterval)
            {
                System.out.println("Burst!");
                List<Routine> burstRoutine = generateRoutine();
                rtnList.addAll(burstRoutine);

                lockTable.register(burstRoutine, timeTick);

                System.out.println("\n-------------------------------------------------------");
                System.out.println("A SINGLE BURST ONLY.....");
                System.out.println("-------------------------------------------------------\n");
                break;
            }
        }
        */

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
        devIDlist.add(DEV_ID.L);
        devIDlist.add(DEV_ID.M);
        devIDlist.add(DEV_ID.N);
        devIDlist.add(DEV_ID.O);

        String logStr = "";

        String dataStorageDirectory = "C:\\Users\\shegufta\\Desktop\\smartHomeData";

        String zipFianStr = prepareZipfian();
        System.out.println(zipFianStr);
        logStr += zipFianStr;

        Map<Double, List<Double>> globalDataCollector = new HashMap<>();
        List<Double> variableTrakcer = new ArrayList<>();
        Double variableInfo = -1.0;
        String lastFilePath = "dummy";

        for(maxConcurrentRtn = 1; maxConcurrentRtn <= 10 ; maxConcurrentRtn++)
        {
//            String zipFianStr = prepareZipfian();
//            System.out.println(zipFianStr);
//            logStr += zipFianStr;

            String fileNameHint = "maxConcurrentRtn";
            variableInfo = (double)maxConcurrentRtn;
            variableTrakcer.add(variableInfo); // add the variable name
            List<Double> resultCollector = new ArrayList<>();



            System.out.println("--------------------------------");
            logStr += "--------------------------------\n";
            System.out.println("TOTAL DEV COUNT: = " + devIDlist.size());
            logStr += "TOTAL DEV COUNT: = " + devIDlist.size() + "\n";
            System.out.println("totalConcurrentRtn = " + maxConcurrentRtn);
            logStr += "totalConcurrentRtn = " + maxConcurrentRtn + "\n";
            System.out.println("maxCommandPerRtn = " + maxCommandPerRtn);
            logStr += "maxCommandPerRtn = " + maxCommandPerRtn + "\n";
            //System.out.println("longRunningProbability = " + longRunningProbability + " NOTE: in current version (using generateFixedPatternRtn) it does not matter!");
            //logStr += "longRunningProbability = " + longRunningProbability + " NOTE: in current version (using generateFixedPatternRtn) it does not matter!" + "\n";
            System.out.println("longRunningCmdDuration = " + longRunningCmdDuration);
            logStr += "longRunningCmdDuration = " + longRunningCmdDuration + "\n";
            System.out.println("shortCmdDuration = " + shortCmdDuration);
            logStr += "shortCmdDuration = " + shortCmdDuration + "\n";

            System.out.println("zipfCoefficient = " + zipfCoefficient);
            logStr += "zipfCoefficient = " + zipfCoefficient + "\n";
            System.out.println("longRunningRtnPercentage = " + longRunningRtnPercentage);
            logStr += "longRunningRtnPercentage = " + longRunningRtnPercentage + "\n";
            System.out.println("atleastOneLongRunning = " + atleastOneLongRunning);
            logStr += "atleastOneLongRunning = " + atleastOneLongRunning + "\n";
            System.out.println("totalSampleCount = " + totalSampleCount);
            logStr += "totalSampleCount = " + totalSampleCount + "\n";

            System.out.println("devFailureRatio = " + devFailureRatio);
            logStr += "devFailureRatio = " + devFailureRatio + "\n";
            System.out.println("atleastOneDevFail = " + atleastOneDevFail);
            logStr += "atleastOneDevFail = " + atleastOneDevFail + "\n";

            System.out.println("mustCmdPercentage = " + mustCmdPercentage);
            logStr += "mustCmdPercentage = " + mustCmdPercentage + "\n";






//            //////////////////////
//            logStr += zipFianStr;
//            /////////////////////



            System.out.println("--------------------------------");
            logStr += "--------------------------------\n";



            String fileUniqueID = System.currentTimeMillis() + "_";
            String fileNameAttributes = "TotalDev_" + devIDlist.size() +
                    "RtnCnt_" + maxConcurrentRtn +
                    "CmdCnt_" + maxCommandPerRtn +
                    "LRLen_" + longRunningCmdDuration +
                    "ZipF_" + (int)(zipfCoefficient*100) +
                    "LongRPrcnz_" + longRunningRtnPercentage +
                    "atlstOneLR_" + atleastOneLongRunning;

            String fileName = fileUniqueID + fileNameHint + fileNameAttributes + ".dat";
            String filePath = dataStorageDirectory + "\\" + fileName;
            lastFilePath = filePath;

            List<Double> allDelay_Strong_List = new ArrayList<>();
            List<Double> allGap_Strong_List = new ArrayList<>();
            List<Double> abortRatio_StrongList = new ArrayList<>();
            List<Double> recoverCmdRatio_StrongList = new ArrayList<>();

            List<Double> allDelay_RelStrong_List = new ArrayList<>();
            List<Double> allGap_RelStrong_List = new ArrayList<>();
            List<Double> abortRatio_RelStrongList = new ArrayList<>();
            List<Double> recoverCmdRatio_RelStrongList = new ArrayList<>();

            List<Double> allDelay_Eventual_List = new ArrayList<>();
            List<Double> allGap_Eventual_List = new ArrayList<>();
            List<Double> abortRatio_Eventual_List = new ArrayList<>();
            List<Double> recoverCmdRatio_Eventual_List = new ArrayList<>();

            int resolution = 10;
            int stepSize = totalSampleCount / resolution;
            if(stepSize == 0)
                stepSize = 1;

            for(int I = 0 ; I < totalSampleCount ; I++)
            {
                if(I == totalSampleCount - 1)
                {
                    System.out.println("variableInfo = " + variableInfo + " | 100%");
                }
                else if(totalSampleCount % stepSize == 0)
                {
                    System.out.println("variableInfo = " + variableInfo + " | " + (int) (100.0 * ((double)I / (double)totalSampleCount)) + "%");
                }

                //List<Routine> routineSet = generateFixedPatternRtn(0);
                List<Routine> routineSet = generateAutomatedRtn(-1);


                //logStr += printInitialRoutineList(routineSet) + "\n";

                FailureResult failureResult;
                double abtRatio;
                double recoverCmdRatio;

                ExpResults expStrong = runExperiment(devIDlist, CONSISTENCY_TYPE.STRONG, routineSet);
                allDelay_Strong_List.addAll(expStrong.delayList);
                allGap_Strong_List.addAll(expStrong.stretchRatioList);

                failureResult = expStrong.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                if(abtRatio != 0)
                    abortRatio_StrongList.add(abtRatio);
                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                if(recoverCmdRatio != 0)
                    recoverCmdRatio_StrongList.add(recoverCmdRatio);
                //logStr += expStrong.logString + "\n";



                ExpResults expRelaxedStrng = runExperiment(devIDlist, CONSISTENCY_TYPE.RELAXED_STRONG, routineSet);
                allDelay_RelStrong_List.addAll(expRelaxedStrng.delayList);
                allGap_RelStrong_List.addAll(expRelaxedStrng.stretchRatioList);

                failureResult = expRelaxedStrng.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                if(abtRatio != 0)
                    abortRatio_RelStrongList.add(abtRatio);
                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                if(recoverCmdRatio != 0)
                    recoverCmdRatio_RelStrongList.add(recoverCmdRatio);

                //logStr += expRelaxedStrng.logString + "\n";

                ExpResults expEventual = runExperiment(devIDlist, CONSISTENCY_TYPE.EVENTUAL, routineSet);
                allDelay_Eventual_List.addAll(expEventual.delayList);
                allGap_Eventual_List.addAll(expEventual.stretchRatioList);

                failureResult = expEventual.failureAnalyzer.simulateFailure(devFailureRatio, atleastOneDevFail);
                abtRatio = failureResult.getAbtRtnVsTotalRtnRatio(maxConcurrentRtn);
                if(abtRatio != 0)
                    abortRatio_Eventual_List.add(abtRatio);
                recoverCmdRatio = failureResult.getRecoveryCmdSentRatio();
                if(recoverCmdRatio != 0)
                    recoverCmdRatio_Eventual_List.add(recoverCmdRatio);

                //logStr += expEventual.logString + "\n";



                //runExperiment(devIDlist, CONSISTENCY_TYPE.WEAK, routineSet);
            }

            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~all runs~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            logStr += "~~~~~~~~~~~~~~~~~~~~~~~~~~~all runs~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ExpResults allDelay_StrongExpRslt = getStats("ALL STRONG DELAY", allDelay_Strong_List);
            String allDelay_Strong_stats = allDelay_StrongExpRslt.logString;

            ExpResults allDelay_RelaxedStrongExpRslt = getStats("ALL R.STRONG DELAY", allDelay_RelStrong_List);
            String allDelay_RelaxedStrong_stats = allDelay_RelaxedStrongExpRslt.logString;

            ExpResults allDelay_EventualExpRslt = getStats("ALL EVENTUAL DELAY", allDelay_Eventual_List);
            String allDelay_Eventual_stats = allDelay_EventualExpRslt.logString;
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ExpResults stretchRatio_EventualExpRslt = getStats("ALL EVENTUAL STRETCH_RATIO", allGap_Eventual_List);
            String stretchRatio_Eventual_stats = stretchRatio_EventualExpRslt.logString;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ExpResults abortRatio_StrongExpRslt = getStats("ABT RATIO STRONG", abortRatio_StrongList);
            String abortRatio_Strong_stats = abortRatio_StrongExpRslt.logString;

            ExpResults abortRatio_RelStrongRslt = getStats("ABT RATIO R.STRONG", abortRatio_RelStrongList);
            String abortRatio_RelStrong_stats = abortRatio_RelStrongRslt.logString;

            ExpResults abortRatio_EventualRslt = getStats("ABT RATIO EVENTUAL", abortRatio_Eventual_List);
            String abortRatio_Eventual_stats = abortRatio_EventualRslt.logString;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            ExpResults recoverCmdRatio_StrongExpRslt = getStats("Recover Cmd RATIO STRONG", recoverCmdRatio_StrongList);
            String recoverCmdRatio_Strong_stats = recoverCmdRatio_StrongExpRslt.logString;

            ExpResults recoverCmdRatio_RelStrongRslt = getStats("Recover Cmd RATIO R.STRONG", recoverCmdRatio_RelStrongList);
            String recoverCmdRatio_RelStrong_stats = recoverCmdRatio_RelStrongRslt.logString;

            ExpResults recoverCmdRatio_EventualRslt = getStats("Recover Cmd RATIO EVENTUAL", recoverCmdRatio_Eventual_List);
            String recoverCmdRatio_Eventual_stats = recoverCmdRatio_EventualRslt.logString;



            System.out.println(allDelay_Strong_stats);
            logStr += allDelay_Strong_stats + "\n";

            System.out.println(allDelay_RelaxedStrong_stats);
            logStr += allDelay_RelaxedStrong_stats + "\n";

            System.out.println(allDelay_Eventual_stats);
            logStr += allDelay_Eventual_stats + "\n";

            System.out.println(stretchRatio_Eventual_stats);
            logStr += stretchRatio_Eventual_stats+ "\n";

            System.out.println(abortRatio_Strong_stats);
            logStr += abortRatio_Strong_stats+ "\n";

            System.out.println(abortRatio_RelStrong_stats);
            logStr += abortRatio_RelStrong_stats+ "\n";

            System.out.println(abortRatio_Eventual_stats);
            logStr += abortRatio_Eventual_stats+ "\n";

            System.out.println(recoverCmdRatio_Strong_stats);
            logStr += recoverCmdRatio_Strong_stats+ "\n";

            System.out.println(recoverCmdRatio_RelStrong_stats);
            logStr += recoverCmdRatio_RelStrong_stats+ "\n";

            System.out.println(recoverCmdRatio_Eventual_stats);
            logStr += recoverCmdRatio_Eventual_stats+ "\n";


            logStr += "\n\n=========================================================================\n\n";
            //String header = "Variable \t StgAvg \t StgSD \t R.StgAvg \t R.StgSD \t EvnAvg \t EvnSD \t EvnGapAvg \t EvnGapSD";
            resultCollector.add((double)allDelay_StrongExpRslt.roundedAvg);
            resultCollector.add((double)allDelay_StrongExpRslt.roundedSD);
            resultCollector.add((double)allDelay_RelaxedStrongExpRslt.roundedAvg);
            resultCollector.add((double)allDelay_RelaxedStrongExpRslt.roundedSD);
            resultCollector.add((double)allDelay_EventualExpRslt.roundedAvg);
            resultCollector.add((double)allDelay_EventualExpRslt.roundedSD);
            resultCollector.add((double)stretchRatio_EventualExpRslt.roundedAvg);
            resultCollector.add((double)stretchRatio_EventualExpRslt.roundedSD);

            resultCollector.add(abortRatio_StrongExpRslt.rawAvg);
            resultCollector.add(abortRatio_RelStrongRslt.rawAvg);
            resultCollector.add(abortRatio_EventualRslt.rawAvg);
            resultCollector.add(recoverCmdRatio_StrongExpRslt.rawAvg);
            resultCollector.add(recoverCmdRatio_RelStrongRslt.rawAvg);
            resultCollector.add(recoverCmdRatio_EventualRslt.rawAvg);



            globalDataCollector.put(variableInfo, resultCollector);


        }

        String globalResult = "\n--------------------------------\n";
        String header = "Variable\tStgAvg\tStgSD\tR.StgAvg\tR.StgSD\tEvnAvg\tEvnSD\tEvnStretchRatioAvg\tEvnStretchRatioSD";
        header += "\tStgAbtAvg\tRStgAbtAvg\tEvnAbtAvg\tStgRcvrRatioAvg\tR.StgRcvrRatioAvg\tEvnRecvrRatioAvg";
        globalResult += header + "\n";
        for(double variable : variableTrakcer)
        {
            globalResult += variable + "\t";

            for(double stats : globalDataCollector.get(variable))
            {
                if(stats < 1.0)
                {
                    String formattedNumber = String.format("%.2f", stats);
                    globalResult += formattedNumber + "\t";
                }
                else
                {
                    globalResult += (int)stats + "\t";
                }
            }

            globalResult += "\n";
        }
        globalResult += "--------------------------------\n";

        System.out.println(globalResult);
        logStr += globalResult;

        try
        {
            Writer fileWriter = new FileWriter(lastFilePath);
            fileWriter.write(logStr);
            fileWriter.close();

        } catch (IOException e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }



    }

}
