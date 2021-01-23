/**
 * Metadata class for SafeHome Failure Analyzer.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 10-Oct-19
 * @time 6:37 PM
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

import SafeHomeSimulator.Command;
import SafeHomeSimulator.DEV_ID;
import SafeHomeSimulator.Routine;

import java.util.Comparator;
import java.util.Set;


public class FailureAnalysisMetadata implements Comparator<FailureAnalysisMetadata>
{
    public int routineID;
    public DEV_ID devID;
    public int cmdStartTime;
    public int cmdEndTime;
    public int rtnStartTime;
    public int rtnEndTime;
    public COMMAND_STATUS commandStatus;
    public ROUTINE_STATUS routineStatus;

    public boolean isMust;

    @Override
    public String toString()
    {
        String str = "";

        String isMustStr = (isMust)? "1" : "0";

        str += " {R-" + routineID +
                ",cmd=[" + cmdStartTime + "-" + cmdEndTime + "],stat=" + commandStatus.name() +
                "rtn=[" + rtnStartTime + "-" + rtnEndTime + "],stat=" + routineStatus.name() +
                "isMust=" + isMustStr + "} ";
        return str;
    }

    public FailureAnalysisMetadata()
    {
        // to support Collections.sort functionality!
    }

    public FailureAnalysisMetadata(final Routine _routine, DEV_ID _devId)
    {
        this.routineID = _routine.ID;
        this.devID = _devId;
        Command cmd = _routine.getCommandByDevID(this.devID);

        assert (cmd != null);
        this.isMust = cmd.isMust;
        this.cmdStartTime = cmd.startTime;
        this.cmdEndTime = cmd.getCmdEndTime();
        this.rtnStartTime = _routine.routineStartTime();
        this.rtnEndTime = _routine.routineEndTime();
        this.routineStatus = ROUTINE_STATUS.UNKNOWN;
    }

    public void updateCommandStatus(Set<DEV_ID> failedDevSet, int failureTime)
    {
        assert(this.rtnStartTime <= failureTime);

        this.routineStatus = ROUTINE_STATUS.UNKNOWN;

        if(this.rtnEndTime <= failureTime) // end time is exclusive
        {
            commandStatus = COMMAND_STATUS.COMMITTED;
        }
        else
        {
            if(!this.isMust)
            {
                commandStatus = COMMAND_STATUS.BEST_EFFORT;
            }
            else
            {
                if(failedDevSet.contains(this.devID))
                {
                    if( this.cmdEndTime <= failureTime) // end time is exclusive
                        commandStatus = COMMAND_STATUS.FAILED_AFTER_EXECUTION;
                    else if(failureTime < this.cmdStartTime)
                        commandStatus = COMMAND_STATUS.FAILED_BEFORE_EXECUTION;
                    else
                        commandStatus = COMMAND_STATUS.FAILED_DURING_EXECUTION;
                }
                else
                {
                    commandStatus = COMMAND_STATUS.NOT_FAILED;
                }
            }
        }
    }

    @Override
    public int compare(FailureAnalysisMetadata a, FailureAnalysisMetadata b)
    {
        return (a.cmdStartTime < b.cmdStartTime) ? -1 : ( (a.cmdStartTime == b.cmdStartTime)? 0 : 1 );
    }
}
