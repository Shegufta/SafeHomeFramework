package SafeHome;

import ConcurrencyController.ConcurrencyControllerSingleton;
import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.Command;
import Utility.DEV_ID;
import Utility.DEV_STATUS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/20/2019
 * @time 7:41 AM
 */
public class SafeHome
{
    public SafeHome(Set<DEV_ID> _devIDset)
    {
        ConcurrencyControllerSingleton.getInstance().initDeviceList(_devIDset); //Init Concurrency Controller
    }

    public void registerRoutine(SelfExecutingRoutine newRoutine)
    {
        ConcurrencyControllerSingleton.getInstance().registerRoutine(newRoutine);
    }

    public static void main(String[] args)
    {
        SafeHome safeHome = new SafeHome(getDevIDSet());

        System.out.println("=============================");
        safeHome.registerRoutine(getRoutine1());
        System.out.println("=============================");


        try
        {
            Thread.sleep(99999999);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }


    }

    public static SelfExecutingRoutine getRoutine1()
    {
        List<Command> cmdChain = new ArrayList<>();

        cmdChain.add(new Command(DEV_ID.FAN, DEV_STATUS.ON, 0));
        //cmdChain.add(new Command(DEV_ID.DUMMY_WAIT, DEV_STATUS.NOT_INITIALIZED, 5000));
        cmdChain.add(new Command(DEV_ID.LIGHT, DEV_STATUS.ON, 0));
        cmdChain.add(new Command(DEV_ID.FAN, DEV_STATUS.OFF, 0));

        SelfExecutingRoutine selfExcRtn = new SelfExecutingRoutine();
        selfExcRtn.addCmdChain(cmdChain);

        return selfExcRtn;
    }

    public static Set<DEV_ID> getDevIDSet()
    {
        Set<DEV_ID> devSet = new HashSet<>();

        devSet.add(DEV_ID.FAN);
        devSet.add(DEV_ID.LIGHT);

        return devSet;
    }
}
