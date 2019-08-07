package Temp;


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
    public static final int COMMAND_DURATION_SEC = 1;

    private static final int maxConcurrentRtn = 4; //in current version totalConcurrentRtn = maxConcurrentRtn;
    private static final int maxCommandPerRtn = 4; // in current version totalCommandInThisRtn = maxCommandPerRtn;
    private static final double longRunningProbability = 0.5;//1.0;
    private static final int longRunningCmdDuration = 100;
    private static final int shortCmdDuration = 1;
    //private static final int simulationTimeUnit = 1000;
    //private static final int rutineBurstInterval = 100;

    private static List<DEV_ID> devIDlist = new ArrayList<>();


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

                Command cmd = new Command(devID, devIDDurationMap.get(devID));
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

    private static List<Routine> generateRoutine(int nonNegativeSeed)
    {
        /*
        List<Routine> rtnList = new ArrayList<>();

        Routine rtn;

        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.A, 1));
        rtn.addCommand(new Command(DEV_ID.B, 100));
        rtnList.add(rtn);

        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.C, 1));
        rtn.addCommand(new Command(DEV_ID.D, 1));
        rtnList.add(rtn);

        return rtnList;
        */


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

    public static String getStats(String itemName, List<Integer> list)
    {
        double itemCount = list.size();

        double sum = 0.0;

        for(int item : list)
        {
            sum += item;
        }

        double avg = 0.0;

        if(0 < itemCount)
        {
            avg = sum / itemCount;
        }

        sum = 0.0;
        for(int item : list)
        {
            sum += (item - avg)*(item - avg);
        }

        if(0 < itemCount)
        {
            sum = sum / itemCount;
        }

        double standardDeviation = Math.sqrt(sum);

        String str = String.format( "%20s",itemName);
        str += ": count = " + String.format("%.0f",itemCount);
        str += "; avg = " + String.format("%7.2f",avg);
        str += "; sd = " + String.format("%7.2f",standardDeviation);

        return str;
    }

    public static String runExperiment(List<DEV_ID> _devIDlist, CONSISTENCY_TYPE _consistencyType, List<Routine> _rtnList)
    {
        System.out.println("\n------------------------------------------------------------------" );
        String logString = "\n------------------------------------------------------------------\n";

        System.out.println("---------------------- " + _consistencyType.name() + " ------------------------------");
        logString += "---------------------- " + _consistencyType.name() + " ------------------------------\n";

        LockTable lockTable = new LockTable(_devIDlist, _consistencyType);
        List<Routine> rtnList = new ArrayList<>(_rtnList);

        int timeTick = 0;
        lockTable.register(rtnList, timeTick);

        System.out.println(lockTable.toString());
        logString += lockTable.toString() + "\n";

        System.out.println("----------------------");
        logString += "----------------------\n";

        List<Integer> delayList = new ArrayList<>();
        List<Integer> gapList = new ArrayList<>();

        for(Routine routine : rtnList)
        {
            System.out.println(routine);
            logString += routine + "\n";
            delayList.add(routine.getStartDelay());
            gapList.add(routine.getGapCount());
        }

        System.out.println("----------------------");
        logString += "----------------------\n";

        System.out.println(getStats("DELAY", delayList));
        logString += getStats("DELAY", delayList) + "\n";

        System.out.println(getStats("GAP", gapList));
        logString += getStats("GAP", gapList) + "\n";

        return logString;

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

        String logStr = "";

        System.out.println("--------------------------------");
        logStr += "--------------------------------\n";
        System.out.println("TOTAL DEV COUNT: = " + devIDlist.size());
        logStr += "TOTAL DEV COUNT: = " + devIDlist.size() + "\n";
        System.out.println("totalConcurrentRtn = " + maxConcurrentRtn);
        logStr += "totalConcurrentRtn = " + maxConcurrentRtn + "\n";
        System.out.println("maxCommandPerRtn = " + maxCommandPerRtn);
        logStr += "maxCommandPerRtn = " + maxCommandPerRtn + "\n";
        System.out.println("longRunningProbability = " + longRunningProbability + " NOTE: in current version (using generateFixedPatternRtn) it does not matter!");
        logStr += "longRunningProbability = " + longRunningProbability + " NOTE: in current version (using generateFixedPatternRtn) it does not matter!" + "\n";
        System.out.println("longRunningCmdDuration = " + longRunningCmdDuration);
        logStr += "longRunningCmdDuration = " + longRunningCmdDuration + "\n";
        System.out.println("shortCmdDuration = " + shortCmdDuration);
        logStr += "shortCmdDuration = " + shortCmdDuration + "\n";
        System.out.println("--------------------------------");
        logStr += "--------------------------------\n";

        String dataStorageDirectory = "C:\\Users\\shegufta\\Desktop\\smartHomeData";
        String fileName = "TotalDev_" + devIDlist.size() + "RtnCnt_" + maxConcurrentRtn + "CmdCnt_" + maxCommandPerRtn + "LRLen_" + longRunningCmdDuration + ".dat";
        String filePath = dataStorageDirectory + "\\" + fileName;



        List<Routine> routineSet = generateFixedPatternRtn(0);



        logStr += printInitialRoutineList(routineSet) + "\n";

        logStr += runExperiment(devIDlist, CONSISTENCY_TYPE.STRONG, routineSet) + "\n";
        logStr += runExperiment(devIDlist, CONSISTENCY_TYPE.RELAXED_STRONG, routineSet) + "\n";
        logStr += runExperiment(devIDlist, CONSISTENCY_TYPE.EVENTUAL, routineSet) + "\n";
        //runExperiment(devIDlist, CONSISTENCY_TYPE.WEAK, routineSet);


        try
        {
            Writer fileWriter = new FileWriter(filePath);
            fileWriter.write(logStr);
            fileWriter.close();

            /*
            File newFile = new File(filePath);
            if(!newFile.exists())
            {
                newFile.createNewFile();
            }

            newFile.createNewFile(); // if file already exists will do nothing
            */

        } catch (IOException e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }




    }

}
