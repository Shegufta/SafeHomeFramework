package RegistrationPkg;

import LockTableManager.LockTableMetadata;
import SelfExecutingRoutine.SelfExecutingRoutine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 8:49 AM
 */
public abstract class RegistrationManager
{
    public RegistrationType registrationType;

    public RegistrationManager(RegistrationType _registrationType)
    {
        this.registrationType = _registrationType;
    }

    public abstract LockTableBluePrint getLockTableBluePrint(LockTableMetadata _lockTableMetadata, final SelfExecutingRoutine _newRoutine);
}
