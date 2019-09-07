package Temp;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
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

        finalizePrepareStatsAndGetAvg(variable, consistencyType, measurementType); // this call makes it sure that the list has been finalized

        return this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).statLog;
    }

    public void writeStatsInFile(String parentDirPath, String changingParameterName)
    {
        File parentDir = new File(parentDirPath);
        if(!parentDir.exists())
        {
            System.out.println("\n\n\nERROR: inside MeasurementCollector.java: directore not found: " + parentDirPath);
            System.exit(1);
        }


        for(double variable : variableMeasurementMap.keySet())
        {
            String subDirPartialName = changingParameterName + variable;
            String subDirPath = parentDirPath + "\\" + subDirPartialName;

            File subDir = new File(subDirPath);
            if(!subDir.exists())
                subDir.mkdir();

            for(CONSISTENCY_TYPE consistencyType : this.variableMeasurementMap.get(variable).keySet())
            {
                for(MEASUREMENT_TYPE measurementType : this.variableMeasurementMap.get(variable).get(consistencyType).keySet())
                {
                    String fileName = consistencyType.name() + "_" + measurementType.name() + ".dat";
                    String filePath = subDirPath + "\\" + fileName;

                    writeToFile(filePath, variable, consistencyType, measurementType);
                }
            }
        }

    }

    private String listToString(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        finalizePrepareStatsAndGetAvg(variable, consistencyType, measurementType); // this call makes it sure that the list has been finalized

        String str = "data\tactualFrequency\n";

        double itemCount = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.size();
        final double actualFrequency = 1.0 / itemCount;

        double frequencySum = 0.0;

        for(int I = 0 ; I < itemCount ; ++I)
        {
            double data = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.get(I);
            frequencySum += actualFrequency;

            str += String.format("%.3f", data) + "\t" + String.format("%.3f", frequencySum);;

            if(I < (itemCount -1))
                str += "\n";
        }

        return str;
    }

    private void writeToFile(String filePath, double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        String listToString = listToString(variable, consistencyType, measurementType);
        try
        {
            Writer fileWriter = new FileWriter(filePath);
            fileWriter.write(listToString);
            fileWriter.close();
        }
        catch (Exception ex)
        {
            System.out.println("\n\nERROR: cannot write file " + filePath);
            System.exit(1);
        }

    }
}
