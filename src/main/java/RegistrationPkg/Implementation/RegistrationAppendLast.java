package RegistrationPkg.Implementation;

import LockTableManager.DevBasedRoutineMetadata;
import LockTableManager.LockTableMetadata;
import RegistrationPkg.LockTableBluePrint;
import RegistrationPkg.RegistrationManager;
import RegistrationPkg.RegistrationType;
import SelfExecutingRoutine.SelfExecutingRoutine;
import Utility.DEV_ID;

import java.util.Set;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 8:50 AM
 */
public class RegistrationAppendLast extends RegistrationManager
{
    public RegistrationAppendLast (RegistrationType _registrationType)
    {
        super(_registrationType);
    }

    public LockTableBluePrint getLockTableBluePrint(LockTableMetadata _lockTableMetadata, final SelfExecutingRoutine _newRoutine)
    {
        Set<DEV_ID> devIDtouchedByNewRtn = _newRoutine.getAllTouchedDevID();

        for(DEV_ID devID : devIDtouchedByNewRtn)
        {
            DevBasedRoutineMetadata newRoutinePerDevMetadata = new DevBasedRoutineMetadata(devID, _newRoutine);

            _lockTableMetadata.AddItemAtRowEnd(devID, newRoutinePerDevMetadata); // Append End
        }

        return _lockTableMetadata.getLockTableBluePrint();
    }
}
