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
    private class DataHolder
    {
        public double average = Double.MIN_VALUE;
        public List<Double> dataList;
        public Boolean isListFinalized;
        String statLog = "";

        public DataHolder()
        {
            this.dataList = new ArrayList<>();
            isListFinalized = false;
        }
    }

    private int maxDataPoint;
    Map<Double, Map<CONSISTENCY_TYPE, Map<MEASUREMENT_TYPE, DataHolder>>> variableMeasurementMap;

    public MeasurementCollector(int _maxDataPoint)
    {
        this.maxDataPoint = _maxDataPoint;
        this.variableMeasurementMap = new HashMap<>();
    }

    private void initiate(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        if(!this.variableMeasurementMap.containsKey(variable))
            this.variableMeasurementMap.put(variable, new HashMap<>() );

        if(!this.variableMeasurementMap.get(variable).containsKey(consistencyType))
            this.variableMeasurementMap.get(variable).put(consistencyType, new HashMap<>() );

        if(!this.variableMeasurementMap.get(variable).get(consistencyType).containsKey(measurementType))
            this.variableMeasurementMap.get(variable).get(consistencyType).put(measurementType, new DataHolder());
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


    public void collectData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType, List<Double> measurementData)
    {
        initiate(variable, consistencyType, measurementType);
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.addAll(measurementData);
    }

    private void sortAndTrimLargeData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        //Collections.sort(_dataList);

        int currentListSize = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.size();

        if(maxDataPoint < currentListSize)
        {// requires trimming

            Set<Integer> uniqueIndexSet = new HashSet<>();
            Random rand = new Random();

            while(uniqueIndexSet.size() < maxDataPoint)
            {
                int randIndex = rand.nextInt(currentListSize);

                if(!uniqueIndexSet.contains(randIndex))
                    uniqueIndexSet.add(randIndex);
            }

            List<Double> trimmedList = new ArrayList<>();

            for(int index : uniqueIndexSet)
            {
                trimmedList.add(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.get(index));
            }

            this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.clear();
            this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.addAll(trimmedList);
        }

        Collections.sort(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList);
    }


    public double finalizePrepareStatsAndGetAvg(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        if(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).isListFinalized)
            return this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).average;

        this.sortAndTrimLargeData(variable, consistencyType, measurementType);

        double avg = 0.0;
        double itemCount = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.size();

        for(int I = 0 ; I < itemCount ; ++I)
        {
            avg += this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.get(I);
        }

        avg = (itemCount == 0.0)? 0.0 : avg/itemCount;

        String logString = consistencyType.name() + " : " + measurementType.name() + " -> " ;
        logString += " count = " + String.format("%.0f",itemCount);
        logString += "; avg = " + String.format("%7.2f",avg);

        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).isListFinalized = true;
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).average = avg;
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).statLog = logString;

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

        finalizePrepareStatsAndGetAvg(variable, consistencyType, measurementType);

        return this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).statLog;
    }
}
