/**
 * RegistrationFactory for SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/25/2019
 * @time 8:48 AM
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

import RegistrationPkg.Implementation.RegistrationAppendLast;


public class RegistrationFactory
{
    public static RegistrationManager createRegistrationManager(RegistrationType _registrationType)
    {
        switch(_registrationType)
        {
            case REG_APPEND_LAST:
            {
                return new RegistrationAppendLast(_registrationType);
            }
            default:
            {
                System.out.println("registration type " + _registrationType.name() + " not supported yet!");
                assert(false);

            }
        }

        return null;
    }
}
