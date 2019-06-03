package CentralController;

import Utility.Device;
import Utility.Routine;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:37 AM
 */
public interface ControllerInterface
{
    /**
     * Receive a routine from the outside world
     * @param routine
     * @return true if scheduled successfully, false if invalid routine, or if cannot be scheduled
     */
    public boolean receiveRoutine(Routine routine);


    /**
     * validate the incoming routine
     * @param routine
     * @return true if validate, false otherwise
     */
    public boolean validateRoutine(Routine routine);

    /**
     * send the validated routine to the concurrency controller
     * @param routine
     * @return true if the concurrency controller successfully schedule the routine, false otherwise
     */
    public boolean sendToConcurrencyController(Routine routine);


    /**
     * Notify the lower layer about device failure
     * @param failedDevList
     */
    public void notifyFailure(List<Device> failedDevList);


    /**
     * Notify the lower layer about new devices
     * @param newDevice
     */
    public void deviceJoin(List<Device> newDevice);

    public void initialize();
}
