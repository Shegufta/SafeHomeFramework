/**
 * Concurrency Controller for SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/24/2019
 * @time 3:18 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package ConcurrencyController.ConcurrencyManager;


public abstract class ConcurrencyController
{
    public ConcurrencyControllerType concurrencyControllerType;
    public Boolean isDisposed;

    public ConcurrencyController(ConcurrencyControllerType _concurrencyControllerType)
    {
        this.concurrencyControllerType = _concurrencyControllerType;
        this.isDisposed = false;
    }
}
