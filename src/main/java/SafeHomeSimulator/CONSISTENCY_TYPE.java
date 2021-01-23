/**
 * CONSISTENCY_TYPE for SafeHome
 *
 * CONSISTENCY_TYPE maintains different consistency models for SafeHome.
 * The RELAXED_STRONG represents for PSV in paper.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 31-Jul-19
 * @time 1:18 AM
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


public enum CONSISTENCY_TYPE
{
    SUPER_STRONG,
    STRONG,
    RELAXED_STRONG,
    EVENTUAL,
    WEAK,
    LAZY,
    LAZY_FCFS,
    LAZY_PRIORITY
}
