/**
 * (Deployment related) Device State in SafeHome.
 *
 * Device state currently does not impact how SafeHome models work, but it
 * is a potential aspect that could be further explored.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:26 AM
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


public enum DEV_STATUS
{
    ON,
    OFF,
    FAILED,
    UNKNOWN,
    NOT_INITIALIZED,
    WAIT
}
