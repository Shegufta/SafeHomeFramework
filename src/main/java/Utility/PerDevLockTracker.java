package Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/2/2019
 *
 * @time 11:55 PM
 */
public class PerDevLockTracker
{

    public class RtnIDLckStatusTuple
    {
        public int routineID;
        public Set<Integer> preRoutineSet;
        public Set<Integer> postRoutineSet;
        public DEV_LOCK lockStatus;

        public RtnIDLckStatusTuple(int _routineID, DEV_LOCK _lockStatus, Set<Integer> _preRoutineSet, Set<Integer> _postRoutineSet)
        {
            this.routineID = _routineID;
            this.lockStatus = _lockStatus;
            this.preRoutineSet = new HashSet<>(_preRoutineSet);
            this.postRoutineSet = new HashSet<>(_postRoutineSet);
        }
    }

    public String devName;
    public DEV_STATUS comittedStatus;
    public List<RoutineTracker> accessedRoutineList;

    public PerDevLockTracker(String _devName, DEV_STATUS _devStatus)
    {
        this.devName = _devName;
        this.comittedStatus = _devStatus;
        this.accessedRoutineList = new ArrayList<>();
    }

    public List<RtnIDLckStatusTuple> getLockStatus()
    {
        List<RtnIDLckStatusTuple> routineIDLckStatusList = new ArrayList<>();

        for(RoutineTracker routineTracker : this.accessedRoutineList)
        {
            routineIDLckStatusList.add(
                    new RtnIDLckStatusTuple(routineTracker.getRoutineID(),
                            routineTracker.getDevLockStatus(this.devName),
                            routineTracker.preRoutineSet,
                            routineTracker.postRoutineSet)
            );
        }

        return routineIDLckStatusList;
    }

    public void registerRoutine(RoutineTracker _routineTracker)
    {
        int index;
        for(index = accessedRoutineList.size() - 1 ; 0 <= index ; --index)
        {
            RoutineTracker routineTracker = accessedRoutineList.get(index);

            if(routineTracker.getDevLockStatus(this.devName) == DEV_LOCK.RELEASED
                    ||
                    routineTracker.getDevLockStatus(this.devName) == DEV_LOCK.EXECUTING
            )
            {
                break;
            }
        }

        if(index < 0)
            index = 0;

        this.accessedRoutineList.add(index, _routineTracker);
    }

    public synchronized boolean commitRoutine(int _routineID)
    {
        synchronized (this)
        {
            int index;
            RoutineTracker routineTracker = null;
            for(index = accessedRoutineList.size() - 1 ; 0 <= index ; --index)
            {
                if(accessedRoutineList.get(index).getRoutineID() == _routineID)
                {
                    routineTracker = accessedRoutineList.get(index);
                    break;
                }
            }

            if(routineTracker != null)
            {
                assert(routineTracker.getDevLockStatus(this.devName) == DEV_LOCK.RELEASED
                        ||
                        routineTracker.getDevLockStatus(this.devName) == DEV_LOCK.EXECUTING
                );

                this.comittedStatus = routineTracker.todoFixThisApproach_getLastUsedDesiredStatus(this.devName); // todo: fix this approach
                accessedRoutineList.subList(0, index+1).clear();

                return true;
            }
            else
            {
                return false; // routine not found... Maybe removed by earlier commit!
            }
        }
    }

    public boolean abortRoutine(int _routineID)
    {
        synchronized (this)
        {
            int index;
            for(index = accessedRoutineList.size() - 1 ; 0 <= index ; --index)
            {
                if(accessedRoutineList.get(index).getRoutineID() == _routineID)
                {
                    break;
                }
            }

            if(0 <= index)
            {
                accessedRoutineList.remove(index);
                return true;
            }
            else
                return false;
        }
    }
}
