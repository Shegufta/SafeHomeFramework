package ConcurrencyController;

import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.DEV_ID;
import Utility.DEV_LOCK;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/24/2019
 * @time 12:31 PM
 */
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
}
