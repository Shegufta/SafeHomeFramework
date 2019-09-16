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
        //public float average = Float.MIN_VALUE;
        public List<Float> dataList;

        public boolean isHistogramMode;
        private float HISTOGRAM_KEY_RESOLUTION = 1000.0f; /// convert 3.14159 to 3.14100000... so now 3.14159 and 3.14161 are same... This will save space
        public Map<Float,Integer> globalHistogram;
        List<Float> cdfDataListInHistogramMode = new ArrayList<>();
        List<Float> cdfFrequencyListInHisotgramMode = new ArrayList<>();

        public Boolean isListFinalized;
        int globalItemCount;
        float globalSum;

        public DataHolder()
        {
            this.dataList = new ArrayList<>();
            this.globalHistogram = new HashMap<>();

            this.isListFinalized = false;
            this.isHistogramMode = false;
            this.globalItemCount = 0;
            this.globalSum = 0.0f;
        }

        public float getNthDataOrMinusOne(int N, float dummyMaxItemCount)
        {
            if( (N < 0) || (this.cdfListSize() == 0) || (this.cdfListSize() <= N))
                    return -1;

            if (isHistogramMode)
                return cdfDataListInHistogramMode.get(N);
            else
                return dataList.get(N);
        }

        public float getNthCDFOrMinusOne(int N)
        {
            if( (N < 0) || (this.cdfListSize() == 0) || (this.cdfListSize() <= N))
                return -1;

            if(isHistogramMode)
                return cdfFrequencyListInHisotgramMode.get(N);
            else
            {
                float frequency = 1.0f / this.cdfListSize();
                return frequency * (N + 1.0f);
            }
        }

        public float getAverage()
        {
            return (globalItemCount == 0.0f)? 0.0f : this.globalSum/globalItemCount ;
        }

        public int cdfListSize()
        {
            assert(this.isListFinalized); // should not call until finalize.
            if(this.isHistogramMode)
            {
                assert(!cdfDataListInHistogramMode.isEmpty());
                return cdfDataListInHistogramMode.size();
            }
            else
            {
                return this.globalItemCount;
            }
        }

        public void addData(List<Float> measurementData)
        {
            this.isHistogramMode = false;
            this.dataList.addAll(measurementData);

            for(float data: measurementData)
            {
                this.dataList.add(data);
                this.globalSum += data;
                this.globalItemCount++;
            }
        }

        public void addData(Map<Float,Integer> partialHistogram)
        {
            this.isHistogramMode = true;
            this.dataList = null; // to prevent it from being accidentally used!

            for(Map.Entry<Float, Integer> entry : partialHistogram.entrySet())
            {
                Float data = entry.getKey();
                Integer partialFrequency = entry.getValue();

                data = (float)((int)(data * HISTOGRAM_KEY_RESOLUTION))/ HISTOGRAM_KEY_RESOLUTION; /// convert 3.141592654 to 3.14100000
                /// convert 3.14159 to 3.141... so now 3.14159 and 3.14161 both are 3.141... both will go to the same Map-bucket. This will save huge space

                this.globalSum += data;
                this.globalItemCount += partialFrequency;

                Integer currentDataFrequency = globalHistogram.get(data);

                if(currentDataFrequency == null)
                    globalHistogram.put(data, partialFrequency);
                else
                    globalHistogram.put(data, (partialFrequency + currentDataFrequency));
            }
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

        List<Float> tempList = new ArrayList<>();
        tempList.add((float)measurementData);

        this.collectData(variable, consistencyType, measurementType, tempList);
    }


    public void collectData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType, List<Float> measurementData)
    {
        initiate(variable, consistencyType, measurementType);
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).addData(measurementData);
    }

    public void collectData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType, Map<Float, Integer> histogram)
    {
        initiate(variable, consistencyType, measurementType);
        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).addData(histogram);
    }

    private void sortAndTrimAndAverageAndFinalizeLargeData(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        System.out.println("\tFinalizing for variable = " + variable + "; consistencyType = " + consistencyType + "; measurementType = " + measurementType);
        if(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).isHistogramMode)
        {

            int currentHistogramSize  = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalItemCount;
            final int maxDataPointForHistogram = maxDataPoint;

            System.out.println("\t\tHistogram mode: currentHistogramSize = " + currentHistogramSize + " | maxDataPointForHistogram = " + maxDataPointForHistogram);

            if(maxDataPointForHistogram < currentHistogramSize)
            {

                float[] tempDataArray = new float[maxDataPointForHistogram];

                Set<Integer> uniqueIndexSet = new HashSet<>();
                Random rand = new Random();

                while(uniqueIndexSet.size() < maxDataPointForHistogram)
                {
                    int randHistogramPosition = 1 + rand.nextInt(currentHistogramSize); // random number from 1 to currentHistogramSize

                    if(!uniqueIndexSet.contains(randHistogramPosition))
                        uniqueIndexSet.add(randHistogramPosition);
                }

                assert(tempDataArray.length == uniqueIndexSet.size());

                int trimmedIndex = 0;
                for(int randHistogramPosition : uniqueIndexSet)
                {
                    int I = 0;
                    for(Map.Entry<Float, Integer> entry : this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.entrySet())
                    {
                        Float data = entry.getKey();
                        Integer frequency = entry.getValue();

                        I += frequency;

                        if( randHistogramPosition <= I)
                        {
                            tempDataArray[trimmedIndex++] = data;
                            break;
                        }
                    }
                }

                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalItemCount = 0;
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalSum = 0.0f;
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.clear();

                for(float data : tempDataArray)
                {
                    //float data = tempDataArray[index];//tempList.get(index);
                    Integer currentFrequency = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.get(data);

                    if(currentFrequency == null)
                        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.put(data, 1);
                    else
                        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.put(data, currentFrequency + 1);

                    this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalItemCount++;
                    this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalSum += data;
                }
            }


            List<Float> sortedDataSequenceFromHistogram = new ArrayList<>();

            for(float data : this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.keySet())
            {
                sortedDataSequenceFromHistogram.add(data);
            }

            Collections.sort(sortedDataSequenceFromHistogram);

            int indexTracker = 1;
            final float frequencyMultiplyer = 1.0f / this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalItemCount; // NOTE: this is total item count... not the CDF list size... that CDF list has not been initialized yet!

            for(float sortedData : sortedDataSequenceFromHistogram)
            {
                float frequency = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.get(sortedData);

                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).cdfDataListInHistogramMode.add(sortedData);
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).cdfFrequencyListInHisotgramMode.add(indexTracker * frequencyMultiplyer);

                if(1 < frequency)
                {
                    indexTracker = indexTracker + (int)frequency - 1;
                    this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).cdfDataListInHistogramMode.add(sortedData);
                    this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).cdfFrequencyListInHisotgramMode.add(indexTracker * frequencyMultiplyer);
                }

                indexTracker++;
            }

            this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram.clear();
            this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalHistogram = null;
        }
        else
        {
            System.out.println("\t\tList mode");
            int currentListSize = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.size();
            System.out.println("\t\tHistogram mode: currentListSize = " + currentListSize + " | maxDataPoint = " + maxDataPoint);


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

                List<Float> trimmedList = new ArrayList<>();

                float newSum = 0.0f;
                int newCount = 0;
                for(int index : uniqueIndexSet)
                {
                    float selectedData = this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.get(index);
                    trimmedList.add(selectedData);

                    newSum += selectedData;
                    newCount++;
                }

                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.clear();
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList.addAll(trimmedList);
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalSum = newSum;
                this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).globalItemCount = newCount;
            }

            Collections.sort(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).dataList);
        }

        this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).isListFinalized = true;
    }


    public float finalizePrepareStatsAndGetAvg(double variable, CONSISTENCY_TYPE consistencyType, MEASUREMENT_TYPE measurementType)
    {
        if(this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).isListFinalized)
            return this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).getAverage();

        this.sortAndTrimAndAverageAndFinalizeLargeData(variable, consistencyType, measurementType);

        return this.variableMeasurementMap.get(variable).get(consistencyType).get(measurementType).getAverage();
    }


    public void writeStatsInFile(String parentDirPath, String changingParameterName)
    {
        File parentDir = new File(parentDirPath);
        if(!parentDir.exists())
        {
            System.out.println("\n\n\nERROR: inside MeasurementCollector.java: directory not found: " + parentDirPath);
            System.exit(1);
        }

        List<CONSISTENCY_TYPE> CONSISTENCY_ORDERING = new ArrayList<>();
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.STRONG);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.RELAXED_STRONG);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.EVENTUAL);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.WEAK);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.LAZY);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.LAZY_FCFS);
        CONSISTENCY_ORDERING.add(CONSISTENCY_TYPE.LAZY_PRIORITY);

        Map<CONSISTENCY_TYPE, String> CONSISTENCY_HEADER = new HashMap<>();
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.STRONG, "GSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.RELAXED_STRONG, "PSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.EVENTUAL, "EV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.WEAK, "WV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY, "LV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_FCFS, "LAZY_FCFS");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_PRIORITY, "LAZY_PRIORITY");


        for(double variable : variableMeasurementMap.keySet())
        {
            String subDirPartialName = changingParameterName + variable;
            String subDirPath = parentDirPath + File.separator + subDirPartialName;

            File subDir = new File(subDirPath);
            if(!subDir.exists())
                subDir.mkdir();

            Map<MEASUREMENT_TYPE, List<CONSISTENCY_TYPE> > perMeasurementConsistencyMap = new HashMap<>();
            for(CONSISTENCY_TYPE consistencyType : this.variableMeasurementMap.get(variable).keySet())
            {
                for(MEASUREMENT_TYPE measurementType : this.variableMeasurementMap.get(variable).get(consistencyType).keySet())
                {
                    if(!perMeasurementConsistencyMap.containsKey(measurementType))
                        perMeasurementConsistencyMap.put(measurementType, new ArrayList<>());

                    perMeasurementConsistencyMap.get(measurementType).add(consistencyType);
                }
            }

            for(Map.Entry<MEASUREMENT_TYPE, List<CONSISTENCY_TYPE> > entry : perMeasurementConsistencyMap.entrySet())
            {
                MEASUREMENT_TYPE currentMeasurement = entry.getKey();
                List<CONSISTENCY_TYPE> currentMeasurementAvailableConsistencyList = entry.getValue();

                this.arrangeListsForPrinting(
                        variable,
                        currentMeasurement,
                        currentMeasurementAvailableConsistencyList,
                        subDirPath,
                        CONSISTENCY_ORDERING,
                        CONSISTENCY_HEADER
                );
            }
        }
    }

    private void writeCombinedStatInFile(
            final MEASUREMENT_TYPE currentMeasurement,
            final String subDirPath,
            List<DataHolder> insertedInConsistencyOrder,
            List<String> consistencyHeader
            )
    {

        assert(insertedInConsistencyOrder.size() == consistencyHeader.size());

        String fileName = currentMeasurement.name() + ".dat";
        String filePath = subDirPath + File.separator + fileName;

        float maxItemCount = Integer.MIN_VALUE;

        String combinedCDFStr = "";
        for(int I = 0 ; I < consistencyHeader.size() ; I++)
        {
            if( maxItemCount < insertedInConsistencyOrder.get(I).cdfListSize())
                maxItemCount = insertedInConsistencyOrder.get(I).cdfListSize();

            combinedCDFStr += "data\t" + consistencyHeader.get(I);

            if(I < (consistencyHeader.size() - 1))
                combinedCDFStr += "\t";
            else
                combinedCDFStr += "\n";
        }

        System.out.println("\tPrepared the header: " + combinedCDFStr + "\t\tnext working to extract the data...: maxItemCount = " + maxItemCount);

        for(int N = 0 ; N < maxItemCount ; N++)
        {
            for(int I = 0 ; I < insertedInConsistencyOrder.size() ; I++)
            {
                float data = insertedInConsistencyOrder.get(I).getNthDataOrMinusOne(N, maxItemCount);
                float CDF = insertedInConsistencyOrder.get(I).getNthCDFOrMinusOne(N);

                combinedCDFStr += data + "\t" + CDF;

                if(I < (insertedInConsistencyOrder.size() - 1))
                    combinedCDFStr += "\t";
                else
                    combinedCDFStr += "\n";
            }
        }

        try
        {
            Writer fileWriter = new FileWriter(filePath);
            fileWriter.write(combinedCDFStr);
            fileWriter.close();
        }
        catch (Exception ex)
        {
            System.out.println("\n\nERROR: cannot write file " + filePath);
            System.exit(1);
        }
    }

    private void arrangeListsForPrinting(
            final double variable,
            final MEASUREMENT_TYPE currentMeasurement,
            final List<CONSISTENCY_TYPE> currentMeasurementAvailableConsistencyList,
            final String subDirPath,
            final List<CONSISTENCY_TYPE> CONSISTENCY_ORDERING,
            final Map<CONSISTENCY_TYPE, String> CONSISTENCY_HEADER)
    {

        System.out.println("\nnow arrenging the lists for: variable = " + variable + ", measurement Type  = " + currentMeasurement );

        List<DataHolder> insertedInConsistencyOrder = new ArrayList<>();
        List<String> consistencyHeader = new ArrayList<>();

        for(int I = 0 ; I < CONSISTENCY_ORDERING.size() ; I++)
        {
            CONSISTENCY_TYPE nextConsistencyToPrint = CONSISTENCY_ORDERING.get(I);

            if(currentMeasurementAvailableConsistencyList.contains(nextConsistencyToPrint))
            {
                insertedInConsistencyOrder.add(this.variableMeasurementMap.get(variable).get(nextConsistencyToPrint).get(currentMeasurement));
                consistencyHeader.add(CONSISTENCY_HEADER.get(nextConsistencyToPrint));
            }
        }


        writeCombinedStatInFile(
                currentMeasurement,
                subDirPath,
                insertedInConsistencyOrder,
                consistencyHeader);

    }
}
