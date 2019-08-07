package Temp;

import org.apache.commons.math3.analysis.function.Exp;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Aug-19
 * @time 1:05 AM
 */
public class ExpResults
{
    public String logString;
    public List<Integer> delayList;
    public List<Integer> gapList;

    public ExpResults()
    {
        this.logString = "";
        this.delayList = new ArrayList<>();
        this.gapList = new ArrayList<>();
    }
}
