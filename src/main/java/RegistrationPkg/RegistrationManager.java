/**
 * RegistrationManager for SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 8:49 AM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package RegistrationPkg;

import LockTableManager.LockTableMetadata;
import SelfExecutingRoutine.SelfExecutingRoutine;


public abstract class RegistrationManager
{
    public RegistrationType registrationType;

    public RegistrationManager(RegistrationType _registrationType)
    {
        this.registrationType = _registrationType;
    }

    public abstract LockTableBluePrint getLockTableBluePrint(LockTableMetadata _lockTableMetadata, final SelfExecutingRoutine _newRoutine);
}
