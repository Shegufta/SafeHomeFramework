package CentralController;

import ConcurrencyController.ConcurrencyControllerSingleton;
import ConcurrencyController.LockTable;
import Executor.Executor;
import Utility.Command;
import Utility.DEV_STATUS;
import Utility.Device;
import Utility.Routine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/2/2019
 * @time 11:43 PM
 */
public class Controller// implements ControllerInterface
{
    public static final String DEV_NAME_FAN = "fan";
    public static final String DEV_NAME_LIGHT = "light";
    //public static final String DEV_NAME_TV = "TV";
    public static final String DEV_NAME_WATER_SPRINKLER = "waterSprinkler";
    public static final String DEV_NAME_OVEN = "Oven";

    private Executor executor;

    public Controller()
    {
        this.executor = new Executor();
    }


    public void ReceiveRoutine(Routine _routine)
    {
        //TODO: validate the routine

        this.executor.ReceiveRoutine(_routine);
    }

    public void initialize()
    {
        List<Device> devList = new ArrayList<>();

        devList.add(  new Device(DEV_NAME_FAN, DEV_STATUS.OFF));
        devList.add(  new Device(DEV_NAME_LIGHT, DEV_STATUS.OFF));
        //devList.add(  new Device(DEV_NAME_TV, DEV_STATUS.OFF));
        devList.add(  new Device(DEV_NAME_WATER_SPRINKLER, DEV_STATUS.OFF));
        devList.add(  new Device(DEV_NAME_OVEN, DEV_STATUS.OFF));

        ConcurrencyControllerSingleton.getInstance().InitDeviceList(devList);
    }


    public static void main(String[] args)
    {
        Controller controller = new Controller();
        controller.initialize();

        System.out.println("-----------------");
        controller.ReceiveRoutine(generateRoutine1() );
        System.out.println("-----------------");
        controller.ReceiveRoutine(generateRoutine2() );

        System.out.println("-----------------");
    }

    public static Routine generateRoutine1()
    {// TODO: replace it with automated method
        Routine routine1 = new Routine();
        routine1.addCommand( new Command(DEV_NAME_FAN, DEV_STATUS.ON, 0) );
        routine1.addCommand( new Command(DEV_NAME_OVEN, DEV_STATUS.ON, 5) );
        routine1.addCommand( new Command(DEV_NAME_LIGHT, DEV_STATUS.ON, 0) );

        return routine1;
    }

    public static Routine generateRoutine2()
    {// TODO: replace it with automated method
        Routine routine2 = new Routine();
        routine2.addCommand( new Command(DEV_NAME_WATER_SPRINKLER, DEV_STATUS.ON, 7) );
        routine2.addCommand( new Command(DEV_NAME_LIGHT, DEV_STATUS.ON, 0) );

        return routine2;
    }
}
