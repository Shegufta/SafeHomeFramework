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
    List<Command> commandList;
    int registrationTime = 0;

    //public Set<DEV_ID> devSet;
    public Map<DEV_ID, Boolean> devIdIsMustMap;

    public Routine()
    {
        this.ID = -1;
        this.commandList = new ArrayList<>();
        //this.devSet = new HashSet<>();
        this.devIdIsMustMap = new HashMap<>();
    }

    public void addCommand(Command cmd)
    {
        //assert(!devSet.contains(cmd.devID));
        //devSet.add(cmd.devID);

        assert(!devIdIsMustMap.containsKey(cmd.devID));
        devIdIsMustMap.put(cmd.devID, cmd.isMust);

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

            gap += nextCommand.startTime  - (firstCommand.startTime + firstCommand.duration);
        }

        if(continuousCmdExecutionTime == 0.0)
            return 0.0;

        return (gap + continuousCmdExecutionTime) / continuousCmdExecutionTime;
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
