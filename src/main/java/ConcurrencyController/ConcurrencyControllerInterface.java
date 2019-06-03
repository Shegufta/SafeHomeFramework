package ConcurrencyController;

import Utility.Device;
import Utility.Routine;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:53 AM
 */
public interface ConcurrencyControllerInterface
{
    /**
     * Receive the routine from the central controller
     * @param routine
     * @return true if it can schedule it, false otherwise
     */
    public boolean receiveFromCentralController(Routine routine);


    /**
     * Schedule the incoming routine
     * @param routine
     * @return true if scheduled, false otherwise
     */
    public boolean scheduleRoutine(Routine routine);


    /**
     * Handle device failure
     * @param failedDevList
     */
    public void handleFailure(List<Device> failedDevList);


    /**
     * Handle new device join
     * @param newDevice
     */
    public void deviceJoin(List<Device> newDevice);
}
