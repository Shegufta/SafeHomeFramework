package RegistrationPkg;

import LockTableManager.DevBasedRoutineMetadata;
import LockTableManager.LockTableMetadata;
import Utility.DEV_ID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 11:49 AM
 */
public class LockTableBluePrint
{
    /*
    * NOTE: blueprint CAN NOT remove a routine, it can add a single routine or shuffle existing globalLockTable row
    * */

    public Map<DEV_ID, List<Integer>> devID_routineIDList_Map;

    public LockTableBluePrint(LockTableMetadata _lockTableMetadata)
    {
        this.devID_routineIDList_Map = new ConcurrentHashMap<>();

        Set<DEV_ID> devIDset = _lockTableMetadata.getDevIDset();

        for(DEV_ID devID : devIDset)
        {
            this.devID_routineIDList_Map.put(devID, Collections.synchronizedList(new ArrayList()) );
            for(DevBasedRoutineMetadata metadata : _lockTableMetadata.getRow(devID))
            {
                this.devID_routineIDList_Map.get(devID).add(metadata.routineID);
            }
        }
    }

    public List<Integer> getRoutineIdList(DEV_ID _devId)
    {
        return this.devID_routineIDList_Map.get(_devId);
    }

    public Set<DEV_ID> getAllDevSet()
    {
        return this.devID_routineIDList_Map.keySet();
    }
}
