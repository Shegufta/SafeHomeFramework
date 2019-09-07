package Temp;

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
    //public String logString;
    public List<Double> delayList;
    public List<Double> stretchRatioList;
    public FailureAnalyzer failureAnalyzer;
    public Measurement measurement;

    public double itemCount;
    public double rawAvg;
    public int roundedAvg;
    public double rawSD;
    public int roundedSD;

    public ExpResults()
    {
        //this.logString = "";
        this.delayList = new ArrayList<>();
        this.stretchRatioList = new ArrayList<>();
    }
}
