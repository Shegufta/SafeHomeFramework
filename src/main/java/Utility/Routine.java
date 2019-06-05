package Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 5/20/2019
 * @time 1:22 AM
 */
public class Routine
{
    public int uniqueRoutineID;
    public List<Command> commandList;
    public static int globalRoutineIDallocator = 0;

    public Routine()
    {
        this.commandList = new ArrayList<>();
        this.uniqueRoutineID = Routine.globalRoutineIDallocator;
        Routine.globalRoutineIDallocator++;
    }

    public void addCommand(Command _cmd)
    {
        this.commandList.add(_cmd);
    }
}
