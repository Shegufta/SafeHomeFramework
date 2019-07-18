package Temp;

import Utility.DEV_ID;

import java.util.ArrayList;
import java.util.List;

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

    public Routine()
    {
        this.ID = -1;
        this.commandList = new ArrayList<>();
    }

    public void addCommand(Command cmd)
    {
        this.commandList.add(cmd);
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

    @Override
    public String toString()
    {
        return super.toString();
    }
}
