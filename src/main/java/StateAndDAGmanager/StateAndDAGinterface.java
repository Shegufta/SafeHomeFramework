package StateAndDAGmanager;

import Utility.Device;
import Utility.Routine;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 2:02 AM
 */
public interface StateAndDAGinterface
{
    /**
     * Insert routine to the dag
     * @return true if successfull, false otherwise
     */
    public boolean insertRoutine();


    /**
     * Handle device failure
     * @param failedDevList
     * @return true if successfull, false otherwise
     */
    public boolean handleFailure(List<Device> failedDevList);


    /**
     * Handle new device join
     * @param newDevice
     */
    public void deviceJoin(List<Device> newDevice);


    /**
     * rollback a particular routine
     * @param routine
     * @return
     */
    public boolean rollBack(Routine routine);


    /**
     * Commit a particular routine
     * @param routine
     * @return
     */
    public boolean commit(Routine routine);
}
