/**
 * MetaData for each touched device for each routine in SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/19/2019
 * @time 3:49 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package LockTableManager;

import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.DEV_ID;
import Utility.DEV_LOCK;

import java.util.HashSet;
import java.util.Set;


public class DevBasedRoutineMetadata
{
    public DEV_ID devID;
    public DEV_LOCK devLockStatus;
    public boolean isStarted;// SBA: do not use "boolean", use "Boolean"... this object is used to thread synchronization
    public boolean isDisposed ;
    public int routineID;
    public Set<Integer> preRoutineSet;
    public Set<Integer> postRoutineSet;

    public DevBasedRoutineMetadata(final DEV_ID _devID, final SelfExecutingRoutine routine)
    {
        this.devID = _devID;
        this.devLockStatus = routine.getLockStatus(this.devID);
        this.isStarted = routine.isStarted;
        this.isDisposed = routine.isDisposed;
        this.routineID = routine.routineID;
        this.preRoutineSet = new HashSet<>(routine.preRoutineSet);
        this.postRoutineSet = new HashSet<>(routine.postRoutineSet);
    }

    @Override
    public String toString()
    {
        String str = "[";

        str += "devID:" + this.devID;
        str += "|routineID:" + this.routineID;
        str += "|devLockStatus:" + this.devLockStatus;

        str += "]";

        return str;
    }
}
