/**
 * Command Status for SafeHome commands.
 *
 * COMMAND_STATUS defines the possible status of each command when defining and
 * running in SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 10-Oct-19
 * @time 6:38 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package FailureAnalyzerSimulator;


public enum COMMAND_STATUS
{
    BEST_EFFORT,
    NOT_FAILED,
    COMMITTED,
    FAILED_BEFORE_EXECUTION,
    FAILED_DURING_EXECUTION,
    FAILED_AFTER_EXECUTION,

    IGNORE_USED_IN_EV_ONLY

//    IGNORE,
//    ON_THE_FLY,
//    ABORT,
//    COMMITTED,
//    BEST_EFFORT
}
