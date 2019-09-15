package Temp;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:02 AM
 */
public class Routine implements Comparator<Routine>
{
    public int ID;
    public String abbr = "";
    List<Command> commandList;
    public int registrationTime = 0;
    public Map<DEV_ID, Boolean> devIdIsMustMap;
    public Map<DEV_ID, Command> devIDCommandMap;
    public Map<Integer, Command> indexCommandMap;
    public Map<Command, Integer> commandIndexMap;
    final int ID_NOT_ASSIGNED_YET = -1;
    public float backToBackCmdExecutionWithoutGap;

    public Routine()
    {
        this.ID = ID_NOT_ASSIGNED_YET;
        this.commandList = new ArrayList<>();
        this.devIdIsMustMap = new HashMap<>();
        //this.deviceSet = new HashSet<>();
        this.devIDCommandMap = new HashMap();
        this.indexCommandMap = new HashMap();
        this.commandIndexMap = new HashMap();

        this.backToBackCmdExecutionWithoutGap = 0.0f;
    }

    public Routine(String abbr)
    {
        this.ID = ID_NOT_ASSIGNED_YET;
        this.abbr = abbr;
        this.commandList = new ArrayList<>();
        this.devIdIsMustMap = new HashMap<>();
        //this.deviceSet = new HashSet<>();
        this.devIDCommandMap = new HashMap();
        this.indexCommandMap = new HashMap();
        this.commandIndexMap = new HashMap();

        this.backToBackCmdExecutionWithoutGap = 0.0f;
    }

    public Set<DEV_ID> getAllDevIDSet()
    {
        return this.devIDCommandMap.keySet(); //this.deviceSet;
    }

    public float getBackToBackCmdExecutionTimeWithoutGap()
    {
        return this.backToBackCmdExecutionWithoutGap;
    }

    public void addCommand(Command cmd)
    {
        assert(!devIDCommandMap.containsKey(cmd.devID));

        devIdIsMustMap.put(cmd.devID, cmd.isMust);

        backToBackCmdExecutionWithoutGap += cmd.duration;

        int cmdInsertionIndex = commandList.size();
        this.indexCommandMap.put(cmdInsertionIndex, cmd);
        this.commandIndexMap.put(cmd, cmdInsertionIndex);
        this.devIDCommandMap.put(cmd.devID, cmd);

        this.commandList.add(cmd);
    }


    public float getStretchRatio()
    {
        float gap = 0.0f;

        for(int idx = 0 ; idx < this.commandList.size()-1 ; ++idx)
        {
            Command firstCommand = this.commandList.get(idx);
            Command nextCommand = this.commandList.get(idx + 1);

            gap += nextCommand.startTime  - firstCommand.getCmdEndTime();
        }

        if( this.backToBackCmdExecutionWithoutGap == 0.0f)
            return 0.0f;

        return (gap + backToBackCmdExecutionWithoutGap)/backToBackCmdExecutionWithoutGap;
    }



    public Command getCommandByDevID(DEV_ID devID)
    {
        assert(this.devIDCommandMap.containsKey(devID));

        return this.devIDCommandMap.get(devID);
    }

    int getCommandIndex(DEV_ID devID)
    {
        assert(this.devIDCommandMap.containsKey(devID));

        Command cmd = this.devIDCommandMap.get(devID);

        return this.commandIndexMap.get(cmd);
    }

    DEV_ID getDevID(int cmdIdx)
    {
        return this.commandList.get(cmdIdx).devID;
    }

    public int lockStartTime(DEV_ID _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        return this.devIDCommandMap.get(_devID).startTime;
    }

    public int lockEndTime(DEV_ID _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        Command cmd = this.devIDCommandMap.get(_devID);
        return cmd.startTime + cmd.duration;
    }

    public int lockDuration(DEV_ID _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        return this.devIDCommandMap.get(_devID).duration;
    }

    public int routineStartTime()
    {
        return this.commandList.get(0).startTime;
    }

    private float getEndToEndLatency()
    {
        return this.routineEndTime() - this.registrationTime;
    }

    private float getRoutineExecutionTime()
    {
        return this.routineEndTime() - this.routineStartTime();
    }

    public float getLatencyOverheadPrcnt()
    {
        float endToEndLatency = getEndToEndLatency();
        float overhead = getStartDelay();

        assert(0.0 < endToEndLatency);

        return (overhead/endToEndLatency)*100.0f;
    }

    public float getStartDelay()
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

        if(!this.devIDCommandMap.containsKey(devId))
            return false;

        int cmdStartTimeInclusive = getCommandByDevID(devId).startTime;

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
        deepCopyRoutine.registrationTime = this.registrationTime;

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
        str += "; abbr: " + this.abbr;
        str += "; delay:" + this.getStartDelay();
        str += "; registrationTime: " + this.registrationTime;
        str += "; backTobackExc: " + this.backToBackCmdExecutionWithoutGap;
        str += "; expectedEndWithoutAnyGap: " + (int)(this.registrationTime + this.backToBackCmdExecutionWithoutGap);
        str += "; stretchRatio: " + this.getStretchRatio() + " || ";

        for(Command cmd : this.commandList)
        {
            str += cmd;
        }

        str += " }";

        return str;
    }

    @Override
    public int compare(Routine a, Routine b)
    {
        if(a.registrationTime == b.registrationTime)
        {
            assert(a.ID != b.ID);

            return (a.ID < b.ID) ? -1 : ( (a.ID == b.ID)? 0 : 1 );
        }
        else
        {
            return (a.registrationTime < b.registrationTime) ? -1 : ( (a.registrationTime == b.registrationTime)? 0 : 1 );
        }
    }
}
