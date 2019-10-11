package FailureAnalyzerSimulator;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 10-Oct-19
 * @time 6:38 PM
 */
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
