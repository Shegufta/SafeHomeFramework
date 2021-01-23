/**
 * (Deployment related) Status of device lock in SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/10/2019
 * @time 1:20 AM
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


public enum DEV_LOCK
{
    NOT_ACQUIRED,
    ACQUIRED,
    EXECUTING,
    RELEASED,
    NEVER_ACCESSED
}
