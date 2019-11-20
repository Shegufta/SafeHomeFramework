package Temp;

import SelfExecutingRoutine.SelfExecutingRoutine;

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
    HashMap<DEV_ID, DEV_STATE> trigger = new HashMap<>();
    HashMap<DEV_ID, DEV_STATE> condition = new HashMap<>();
    public int registrationTime = 0;

    // data structure for status maintaining
    public Map<DEV_ID, Boolean> devIdIsMustMap;
    public Map<DEV_ID, Command> devIDCommandMap;
    public Map<Integer, Command> indexCommandMap;
    public Map<Command, Integer> commandIndexMap;
    final int ID_NOT_ASSIGNED_YET = -1;
    public float backToBackCmdExecutionWithoutGap;

    public Map<DEV_ID, List<Integer>> tempPerDevPreSet; // used to calculate the recursive insertion method (EV)
    public Map<DEV_ID, List<Integer>> tempPerDevPostSet; // used to calculate the recursive insertion method (EV)

    public Routine()
    {
        this.initialize();
    }

    private void initialize()
    {
        this.ID = ID_NOT_ASSIGNED_YET;
        this.commandList = new ArrayList<>();
        this.devIdIsMustMap = new HashMap<>();
        //this.deviceSet = new HashSet<>();
        this.devIDCommandMap = new HashMap();
        this.indexCommandMap = new HashMap();
        this.commandIndexMap = new HashMap();

        this.tempPerDevPreSet = new HashMap<>();
        this.tempPerDevPostSet = new HashMap<>();

        this.backToBackCmdExecutionWithoutGap = 0.0f;
    }

    public Routine(String abbr)
    {
        this.initialize();
        this.abbr = abbr;
    }

    public boolean isEmpty() { return this.commandList.isEmpty();}

    public Set<DEV_ID> getAllDevIDSet()
    {
        return this.devIDCommandMap.keySet(); //this.deviceSet;
    }

    public float getBackToBackCmdExecutionTimeWithoutGap()
    {
        return this.backToBackCmdExecutionWithoutGap;
    }

    public void clearTempPrePostSet()
    {// used to calculate the recursive insertion method (EV)
        for(DEV_ID devID : this.devIDCommandMap.keySet())
        {
            tempPerDevPreSet.get(devID).clear();
            tempPerDevPostSet.get(devID).clear();
        }
    }

    public void addCommand(Command cmd)
    {
        assert(!devIDCommandMap.containsKey(cmd.devID));

        tempPerDevPreSet.put(cmd.devID, new ArrayList<>());// used to calculate the recursive insertion method (EV)
        tempPerDevPostSet.put(cmd.devID, new ArrayList<>());// used to calculate the recursive insertion method (EV)

        devIdIsMustMap.put(cmd.devID, cmd.isMust);

        backToBackCmdExecutionWithoutGap += cmd.duration;

        int cmdInsertionIndex = commandList.size();
        this.indexCommandMap.put(cmdInsertionIndex, cmd);
        this.commandIndexMap.put(cmd, cmdInsertionIndex);
        this.devIDCommandMap.put(cmd.devID, cmd);

        this.commandList.add(cmd);
    }

    public void addTrigger(DEV_ID dev_id, DEV_STATE state) {
        this.trigger.put(dev_id, state);
    }

    public boolean hasTrigger() {
        return !this.trigger.isEmpty();
    }

    public void addCondition(DEV_ID dev_id, DEV_STATE state) {
        this.condition.put(dev_id, state);
    }

    public boolean hasCondition() {
        return !this.condition.isEmpty();
    }


    public int getNumberofCommand() {
        return commandList.size();
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

    public float getEndToEndLatency()
    {
        return this.routineEndTime() - this.registrationTime;
    }

    private float getRoutineExecutionTime()
    {
        return this.routineEndTime() - this.routineStartTime();
    }


    public float getE2EvsWaittime()
    {
        float endToEndLatency = getEndToEndLatency();
        float waitTime = getStartDelay();

        assert(0.0 < endToEndLatency);

        if(waitTime == 0.0f)
            return 0.0f;
        else
            return (endToEndLatency/waitTime)*100.0f;
    }

    public float getLatencyOverheadPrcnt()
    {
        float endToEndLatency = getEndToEndLatency();

        assert(backToBackCmdExecutionWithoutGap != 0.0f);

        return (endToEndLatency / backToBackCmdExecutionWithoutGap) * 100.0f;


        /*
        float waitTime = getStartDelay();

        assert(0.0 < endToEndLatency);

        if(waitTime == 0.0f)
            return 0.0f;
        else
            return (endToEndLatency/waitTime)*100.0f;

        */
        //return (overhead/endToEndLatency)*100.0f;

        /*
        * TODO: wait/endtoend
        *  2) latencyOverhead endtoend/minextime
        * 3)TODO: fix dev utilization, + add new dev utilization
        * */
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
        StringBuilder str = new StringBuilder();

        str.append("{ Routine ID:").append(this.ID);
        str.append("; abbr: ").append(this.abbr);
        str.append("; delay:").append(this.getStartDelay());
        str.append("; registrationTime: ").append(this.registrationTime);
        str.append("; backTobackExc: ").append(this.backToBackCmdExecutionWithoutGap);
        str.append("; expectedEndWithoutAnyGap: ").append((int) (this.registrationTime + this.backToBackCmdExecutionWithoutGap));
        str.append("; stretchRatio: ").append(this.getStretchRatio()).append(" || ");

        for(Command cmd : this.commandList)
        {
            str.append(cmd);
        }

        if (!this.trigger.isEmpty()) {
            str.append(" || Trigger: ");
            for (Map.Entry<DEV_ID, DEV_STATE> entry : trigger.entrySet()) {
                str.append(entry.getKey().name()).append(":").append(entry.getValue().name()).append(", ");
            }
        }

        if (!this.trigger.isEmpty()) {
            str.append(" || Condition: ");
            for (Map.Entry<DEV_ID, DEV_STATE> entry : condition.entrySet()) {
                str.append(entry.getKey().name()).append(":").append(entry.getValue().name()).append(", ");
            }
        }

        str.append(" }\n");

        return str.toString();
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
