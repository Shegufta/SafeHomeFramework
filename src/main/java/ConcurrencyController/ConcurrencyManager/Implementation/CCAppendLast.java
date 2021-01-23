/**
 * Concurrency Controller List for SafeHome.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/24/2019
 * @time 3:32 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package ConcurrencyController.ConcurrencyManager.Implementation;


import ConcurrencyController.ConcurrencyManager.ConcurrencyController;
import ConcurrencyController.ConcurrencyManager.ConcurrencyControllerType;


public class CCAppendLast extends ConcurrencyController
{
    public CCAppendLast(ConcurrencyControllerType _controllerType)
    {
        super(_controllerType);
        
    }
}
