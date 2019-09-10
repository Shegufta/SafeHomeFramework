package Temp;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:02 AM
 */
public class Routine
{
    public int ID;
    public int arrivalSequenceForWeakScheduling;
    List<Command> commandList;
    int registrationTime = 0;
    public Map<DEV_ID, Boolean> devIdIsMustMap;
    public Set<DEV_ID> deviceSet;

    public Routine()
    {
        this.ID = -1;
        this.commandList = new ArrayList<>();
        //this.devSet = new HashSet<>();
        this.devIdIsMustMap = new HashMap<>();
        this.deviceSet = new HashSet<>();
    }

    public Set<DEV_ID> getAllDevIDSet()
    {
        return this.deviceSet;
        /*
        Set<DEV_ID> devIDset = new HashSet<DEV_ID>();

        for(Command cmd : commandList)
        {
            devIDset.add(cmd.devID);
        }

        return devIDset;
        */
    }

    public void addCommand(Command cmd)
    {
        //assert(!devSet.contains(cmd.devID));
        //devSet.add(cmd.devID);

        assert(!devIdIsMustMap.containsKey(cmd.devID));
        devIdIsMustMap.put(cmd.devID, cmd.isMust);
        this.deviceSet.add(cmd.devID);

        this.commandList.add(cmd);
    }


    public double getStretchRatio()
    {
        double gap = 0.0;

        double continuousCmdExecutionTime = 0.0;
        for(Command cmd : this.commandList)
        {
            continuousCmdExecutionTime += cmd.duration;
        }

        for(int idx = 0 ; idx < this.commandList.size()-1 ; ++idx)
        {
            Command firstCommand = this.commandList.get(idx);
            Command nextCommand = this.commandList.get(idx + 1);

            gap += nextCommand.startTime  - firstCommand.getCmdEndTime();
        }

        if( continuousCmdExecutionTime == 0.0)
            return 0.0;

        return (gap + continuousCmdExecutionTime)/continuousCmdExecutionTime;
    }



    public Command getCommandByDevID(DEV_ID devID)
    {
        int commandIndex = getCommandIndex(devID);

        if(-1 == commandIndex)
            return null;
        else
            return this.commandList.get(commandIndex);
    }

    int getCommandIndex(DEV_ID devID)
    {
        for(int index = 0 ; index < this.commandList.size() ; index++)
        {
            if(devID == this.commandList.get(index).devID)
                return index;
        }

        System.out.println(devID.name() + " not found");
        assert(false);

        return -1;
    }

    DEV_ID getDevID(int cmdIdx)
    {
        return this.commandList.get(cmdIdx).devID;
    }

    public int lockStartTime(DEV_ID _devID)
    {
        for(Command cmd : commandList)
        {
            if(cmd.devID == _devID)
                return cmd.startTime;
        }

        assert(false);

        return -1;
    }

    public int lockEndTime(DEV_ID _devID)
    {
        return this.lockStartTime(_devID) + this.lockDuration(_devID);
    }

    public int lockDuration(DEV_ID _devID)
    {
        for(Command cmd : commandList)
        {
            if(cmd.devID == _devID)
                return cmd.duration;
        }

        assert(false);

        return -1;
    }

    public int routineStartTime()
    {
        return this.commandList.get(0).startTime;
    }

    private double getEndToEndLatency()
    {
        return this.routineEndTime() - this.registrationTime;
    }

    private double getRoutineExecutionTime()
    {
        return this.routineEndTime() - this.routineStartTime();
    }

    public double getLatencyOverheadPrcnt()
    {
        double endToEndLatency = getEndToEndLatency();
        double overhead = getStartDelay();

        assert(0.0 < endToEndLatency);

        return (overhead/endToEndLatency)*100.0;
    }

    public double getStartDelay()
    {
        return this.routineStartTime() - this.registrationTime;
    }

    public int routineEndTime()
    {
        int lastIdx = commandList.size() - 1;
        assert(0 <= lastIdx);

        Command lastCmd = commandList.get(lastIdx);

        return lastCmd.getCmdEndTime();//lastCmd.startTime + lastCmd.duration;
    }

    public boolean isCommittedByGivenTime(int targetTime)
    {
        //NOTE: end time is exclusive, and start time is inclusive
        return ( this.routineEndTime() == targetTime || this.routineEndTime() < targetTime);
    }

    public boolean isCandidateCmdInsidePreLeaseZone(DEV_ID _devID, int _candidateCmdStartTime, int _candidateCmdDuration)
    {
        int cmdStartTime = getCommandByDevID(_devID).startTime;
        int routineStartTime = this.routineStartTime();
        int candidateCmdEndTime = _candidateCmdStartTime + _candidateCmdDuration;

        if(routineStartTime <= _candidateCmdStartTime && _candidateCmdStartTime <= cmdStartTime)
            return true;

        if(routineStartTime <= candidateCmdEndTime && candidateCmdEndTime <= cmdStartTime)
            return true;

        return false;
    }

    public boolean isCandidateCmdInsidePostLeaseZone(DEV_ID _devID, int _candidateCmdStartTime, int _candidateCmdDuration)
    {
        int cmdEndTime = getCommandByDevID(_devID).getCmdEndTime();
        int routineEndTime = this.routineEndTime();
        int candidateCmdEndTime = _candidateCmdStartTime + _candidateCmdDuration;

        if(cmdEndTime <= _candidateCmdStartTime && _candidateCmdStartTime <= routineEndTime)
            return true;

        if(cmdEndTime <= candidateCmdEndTime && candidateCmdEndTime <= routineEndTime)
            return true;

        return false;
    }

    public boolean isDevAccessStartsDuringTimeSpan(DEV_ID devId, int startTimeInclusive, int endTimeExclusive)
    {
        assert(startTimeInclusive < endTimeExclusive);

        if(!deviceSet.contains(devId))
            return false;

        int cmdStartTimeInclusive = getCommandByDevID(devId).startTime;
        //int cmdEndTimeExclusive = getCommandByDevID(devId).getCmdEndTime();

        if(startTimeInclusive <= cmdStartTimeInclusive && cmdStartTimeInclusive < endTimeExclusive)
            return true;


        return false;
    }


    public boolean isRoutineOverlapsWithGivenTimeSpan(int startTimeInclusive, int endTimeExclusive)
    {
        assert(startTimeInclusive < endTimeExclusive);

        int rtnStartTimeInclusive = this.routineStartTime();
        int rtnEndTimeExclusive = this.routineEndTime();

        if(startTimeInclusive <= rtnStartTimeInclusive && rtnStartTimeInclusive < endTimeExclusive)
            return true;

        if(rtnStartTimeInclusive <= startTimeInclusive && startTimeInclusive < rtnEndTimeExclusive)
            return true;


        return false;
    }


    public Routine getDeepCopy()
    {
        Routine deepCopyRoutine = new Routine();
        deepCopyRoutine.ID = this.ID;

        for(Command cmd : this.commandList)
        {
            deepCopyRoutine.addCommand(cmd.getDeepCopy());
        }

        return deepCopyRoutine;
    }

    @Override
    public String toString()
    {
        String str = "";

        str += "{ Routine ID:" + this.ID;
        str += "; delay:" + this.getStartDelay();
        str += "; stretchRatio:" + this.getStretchRatio() + " || ";

        for(Command cmd : this.commandList)
        {
            str += cmd;
        }

        str += " }";

        return str;
    }
}
