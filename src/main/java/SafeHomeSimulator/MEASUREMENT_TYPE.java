/**
 * Types of metrics that are supported to collect in SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Sep-19
 * @time 7:50 AM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package SafeHomeSimulator;


public enum MEASUREMENT_TYPE
{
    WAIT_TIME,
    BACK2BACK_RTN_CMD_EXCTN_TIME,
    E2E_RTN_TIME,
    LATENCY_OVERHEAD,
    E2E_VS_WAITTIME,
    STRETCH_RATIO,
    PARALLEL_DELTA,
    PARALLEL_RAW,
    ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
    ISVLTN4_CMD_TO_COMMIT_COLLISION_TIMESPAN_PRCNT,
    ISVLTN3_CMD_VIOLATION_PRCNT_PER_RTN,
    ISVLTN2_VIOLATED_RTN_PRCNT,
    ISVLTN1_PER_RTN_COLLISION_COUNT,
    ORDDER_MISMATCH,
    ORDERR_MISMATCH_BUBBLE,
    DEVICE_UTILIZATION,
    ABORT_RATE,
    RECOVERY_CMD_TOTAL,
    RECOVERY_CMD_PER_RTN,
    COMPARE_WV_VS_GSV_END_STATE,
    EV_ROUTINE_INSERT_TIME_MICRO_SEC
    //ON_THE_FLY_CMD
    //EXECUTION_LATENCY_MS
}