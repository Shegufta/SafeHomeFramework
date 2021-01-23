/**
 * ZipfProbBoundary for SafeHome.
 *
 * ZipfProbBoundary checks whether the time is within pre-defined boundaries.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 06-Aug-19
 * @time 11:49 PM
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
