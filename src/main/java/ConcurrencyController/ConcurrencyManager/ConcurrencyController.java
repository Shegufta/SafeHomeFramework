package ConcurrencyController.ConcurrencyManager;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 6/24/2019
 * @time 3:18 PM
 */
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
