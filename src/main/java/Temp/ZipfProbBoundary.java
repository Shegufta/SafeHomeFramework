package Temp;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 06-Aug-19
 * @time 11:49 PM
 */
public class ZipfProbBoundary
{
    public double startExclusive;
    public double endInclusive;

    public ZipfProbBoundary(double _startExclusive, double _endInclusive)
    {
        this.startExclusive = _startExclusive;
        this.endInclusive = _endInclusive;
    }

    public boolean isInsideBoundary(double randDouble)
    {
        assert( 0.0 <= randDouble && randDouble <= 1.0);

        return (this.startExclusive <= randDouble && randDouble < this.endInclusive);
    }
}
