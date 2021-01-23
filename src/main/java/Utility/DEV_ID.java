/**
 * (Deployment related) Device IDs in SafeHome.
 *
 * DEV_ID maintains all the potential IDs that might be touched during
 * experiments. All device names including devices through benchmark need
 * to be added to this enum.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/18/2019
 * @time 5:32 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package Utility;


public enum DEV_ID
{
    LIGHT,
    FAN,
    MICROWAVE,
    DUMMY_WAIT,
    DEV_NOT_ASSIGNED
}
