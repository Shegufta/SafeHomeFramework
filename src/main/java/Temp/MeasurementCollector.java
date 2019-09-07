package Temp;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 07-Sep-19
 * @time 7:47 AM
 */
public class MeasurementCollector
{
    Map<Double, Map<CONSISTENCY_TYPE, Map<MEASUREMENT_TYPE, List<Double>>>> variableMeasurementMap;

    public MeasurementCollector()
    {
        this.variableMeasurementMap = new HashMap<>();
    }

    public void collectData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType, double measurementData, boolean skipZeroValue)
    {
        initiate(variable, consistencyType, measurementType);

        if(measurementData == 0.0 && skipZeroValue)
            return;

        List<Double> tempList = new ArrayList<>();
        tempList.add(measurementData);

        this.collectData(variable, consistencyType, measurementType, tempList);
    }

    private void initiate(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        if(!this.variableMeasurementMap.containsKey(variable))
            this.variableMeasurementMap.put(variable, new HashMap<>() );

        if(!this.variableMeasurementMap.get(variable).containsKey(consistencyType))
            this.variableMeasurementMap.get(variable).put(consistencyType, new HashMap<>() );

        if(!this.variableMeasurementMap.get(variable).get(consistencyType).containsKey(measurementType))
            this.variableMeasurementMap.get(variable).get(consistencyType).put(measurementType, new ArrayList<>());
    }

    public void collectData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType, List<Double> measurementData)
    {
        initiate(variable, consistencyType, measurementType);
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).addAll(measurementData);
    }

    public double getAvg(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        double avg = 0.0;
        double itemCount = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).size();

        for(int I = 0 ; I < itemCount ; ++I)
        {
            avg += this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).get(I);
        }

        avg = (itemCount == 0.0)? 0.0 : avg/itemCount;

        return avg;
    }

    public String getStats(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        if(!this.variableMeasurementMap.containsKey(variable))
            return "ERROR: variable " + variable + " not available (for " + consistencyType.name() + ", " + measurementType.name() + " )";

        if(!this.variableMeasurementMap.get(variable).containsKey(consistencyType))
            return "ERROR: " + consistencyType.name() + " not available!";

        if(!this.variableMeasurementMap.get(variable).get(consistencyType).containsKey(measurementType))
            return "ERROR: " + measurementType.name() + " for " + consistencyType.name() + " not available!";

        String logString = consistencyType.name() + " : " + measurementType.name() + " -> " ;

        double itemCount = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).size();
        double avg = this.getAvg(variable, consistencyType, measurementType);

        logString += " count = " + String.format("%.0f",itemCount);
        logString += "; avg = " + String.format("%7.2f",avg);

        return logString;
    }
}
