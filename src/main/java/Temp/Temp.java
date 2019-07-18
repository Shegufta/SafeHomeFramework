package Temp;

import LockTableManager.LockTableSingleton;
import Utility.DEV_ID;
import Utility.DEV_LOCK;
import Utility.DEV_STATUS;

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

    public static void main (String[] args)
    {
        List<DEV_ID> devIDlist = new ArrayList<>();
        devIDlist.add(DEV_ID.FAN);
        devIDlist.add(DEV_ID.LIGHT);
        devIDlist.add(DEV_ID.MICROWAVE);

        LockTable lockTable = new LockTable(devIDlist);
        //lockTable.generateTestLockTable();


        Routine rtn;

        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.FAN, 1));
        rtn.addCommand(new Command(DEV_ID.MICROWAVE, 100));
        //rtn.addCommand(new Command(DEV_ID.LIGHT, 1));

        lockTable.register(rtn, 0);



        rtn = new Routine();
        rtn.addCommand(new Command(DEV_ID.FAN, 1));
        rtn.addCommand(new Command(DEV_ID.MICROWAVE, 1));

        lockTable.register(rtn, 0);






        System.out.println(lockTable.toString());

        //System.out.println(lockTable.getLockTableEmptyPlaceIndex(DEV_ID.FAN, 0, 5));
    }

}
