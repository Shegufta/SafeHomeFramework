package Temp;


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

    private static final int maxConcurrentRtn = 8;
    private static final int maxCommandPerRtn = 5;
    private static final double longRunningProbability = 1.0;
    private static final int longRunningCmdDuration = 100;
    private static final int shortCmdDuration = 1;
    private static final int simulationTimeUnit = 1000;
    private static final int rutineBurstInterval = 100;

    private static List<DEV_ID> devIDlist = new ArrayList<>();

    private static List<Routine> generateRoutine()
    {
        List<Routine> routineList = new ArrayList<>();
        Random rand = new Random();

        int totalConcurrentRtn = 1 + rand.nextInt(maxConcurrentRtn);

        for(int RoutineCount = 0 ; RoutineCount < totalConcurrentRtn ; ++RoutineCount)
        {
            int totalCommandInThisRtn = 1 + rand.nextInt(maxCommandPerRtn);
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

    public static void main (String[] args)
    {
        devIDlist.add(DEV_ID.A);
        devIDlist.add(DEV_ID.B);
        devIDlist.add(DEV_ID.C);
        devIDlist.add(DEV_ID.D);
        devIDlist.add(DEV_ID.E);
        devIDlist.add(DEV_ID.F);
        devIDlist.add(DEV_ID.G);


        LockTable lockTable = new LockTable(devIDlist);
        List<Routine> rtnList = new ArrayList<>();

        for(int timeTick = 0 ; timeTick <= simulationTimeUnit ; ++timeTick)
        {
            if(0 == timeTick % rutineBurstInterval)
            {
                System.out.println("Burst!");
                List<Routine> burstRoutine = generateRoutine();
                rtnList.addAll(burstRoutine);

                lockTable.register(burstRoutine, timeTick);
            }
        }


        /*
        List<Routine> rtnList = new ArrayList<>();

        Routine rtn;

        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.A, 1));
        rtn.addCommand(new Command(DEV_ID.B, 100));

        lockTable.register(rtn, 0);
        rtnList.add(rtn);



        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.C, 200));

        lockTable.register(rtn, 0);
        rtnList.add(rtn);



        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.B, 1));
        rtn.addCommand(new Command(DEV_ID.C, 1));

        lockTable.register(rtn, 0);
        rtnList.add(rtn);

        */



        System.out.println(lockTable.toString());

        System.out.println("----------------------");

        List<Integer> delayList = new ArrayList<>();
        List<Integer> gapList = new ArrayList<>();

        for(Routine routine : rtnList)
        {
            System.out.println(routine);
            delayList.add(routine.getStartDelay());
            gapList.add(routine.getGapCount());
        }

        System.out.println("----------------------");

        System.out.println(getStats("DELAY", delayList));
        System.out.println(getStats("GAP", gapList));

        //System.out.println(lockTable.getLockTableEmptyPlaceIndex(DEV_ID.FAN, 0, 5));
    }

}
