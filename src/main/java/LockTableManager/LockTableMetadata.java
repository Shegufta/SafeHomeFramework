/**
 * MetaData for SafeHome LockTable
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 9:42 AM
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

import RegistrationPkg.LockTableBluePrint;
import Utility.DEV_ID;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**

 */
public class LockTableMetadata
{
    public Map<DEV_ID, List<DevBasedRoutineMetadata>> metaLockTable;

    public LockTableMetadata()
    {
        this.metaLockTable = new ConcurrentHashMap<>();
    }

    public void AddRow(DEV_ID rowName, List<DevBasedRoutineMetadata> rowData)
    {
        assert(!this.metaLockTable.containsKey(rowName));

        this.metaLockTable.put(rowName, rowData);
    }

    public void AddItemInRow(DEV_ID rowName, int columnIndex, DevBasedRoutineMetadata routineMetadata)
    {
        assert(this.metaLockTable.containsKey(rowName));

        assert( 0 <= columnIndex && columnIndex <= this.metaLockTable.get(rowName).size());

        this.metaLockTable.get(rowName).add(columnIndex, routineMetadata);
    }

    public void AddItemAtRowEnd(DEV_ID rowName, DevBasedRoutineMetadata routineMetadata)
    {
        assert(rowName == routineMetadata.devID);
        assert(this.metaLockTable.containsKey(rowName));
        int listSize = this.metaLockTable.get(rowName).size();
        this.metaLockTable.get(rowName).add(listSize, routineMetadata);
    }

    public Set<DEV_ID> getDevIDset()
    {
        return this.metaLockTable.keySet();
    }

    public List<DevBasedRoutineMetadata> getRow(DEV_ID _devId)
    {
        return this.metaLockTable.get(_devId);
    }

    public LockTableBluePrint getLockTableBluePrint()
    {
        return new LockTableBluePrint(this);
    }

    @Override
    public String toString()
    {
        String str = "--------------------------------\n";

        for(Map.Entry<DEV_ID, List<DevBasedRoutineMetadata>> entry : this.metaLockTable.entrySet())
        {
            str += entry.getKey() + " : ";

            if(entry.getValue().isEmpty())
            {
                str += "Empty...\n";
            }
            else
            {
                for (DevBasedRoutineMetadata metadata : entry.getValue())
                {
                    str += metadata;
                }
                str += "\n";
            }
        }

        str += "--------------------------------\n";

        return str;
    }
}
