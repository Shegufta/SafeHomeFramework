/**
 * (Deployment related) Command for SafeHome.
 *
 * Command includes the structure of a command and the corresponding operations.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:31 AM
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


import static Utility.DEV_ID.DUMMY_WAIT;


public class Command
{
    public DEV_ID devID;
    public DEV_STATUS desiredStatus;
    public int durationMilliSec; // 0 means short command.

    public Command(DEV_ID _devID, DEV_STATUS _desiredStatus, int _durationMilliSec)
    {
        this.devID = _devID;
        this.desiredStatus = _desiredStatus;
        this.durationMilliSec = _durationMilliSec;

        if(0 < this.durationMilliSec)
        {
            assert(this.devID == DUMMY_WAIT);
            assert(this.desiredStatus == DEV_STATUS.WAIT);
        }
        else
        {
            assert(this.devID != DUMMY_WAIT);
            assert(this.desiredStatus != DEV_STATUS.WAIT);
        }
    }

    @Override
    public String toString()
    {
        String str = "[devID = " + this.devID.name() + " | desiredStatus = " + this.desiredStatus + " | durationMilliSec = " + this.durationMilliSec + "]";
        return str;
    }
}
