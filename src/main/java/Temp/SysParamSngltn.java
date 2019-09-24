package Temp;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 23-Sep-19
 * @time 7:15 PM
 */
public class SysParamSngltn
{
    private final String PROPERTY_FILE_NAME = "conf/SafeHomeFramework.config";
    /////////////////////////////////////////////////////////////////////////////////
    private final String KEY_IS_RUNNING_BENCHMARK = "IS_RUNNING_BENCHMARK";
    private final String KEY_TOTAL_SAMPLE_COUNT = "totalSampleCount";
    private final String KEY_IS_PRE_LEASE_ALLOWED = "IS_PRE_LEASE_ALLOWED";
    private final String KEY_IS_POST_LEASE_ALLOWED = "IS_POST_LEASE_ALLOWED";
    private final String KEY_SHRINK_FACTOR = "shrinkFactor";
    private final String KEY_MIN_CMD_CNT_PER_RTN = "minCmdCntPerRtn";
    private final String KEY_MAX_CMD_CNT_PER_RTN = "maxCmdCntPerRtn";
    private final String KEY_ZIPF = "zipF";
    private final String KEY_DEV_REGISTERED_OUT_OF_65_DEV = "devRegisteredOutOf65Dev";
    private final String KEY_MAX_CONCURRENT_RTN = "maxConcurrentRtn";
    private final String KEY_LONG_RTN_PRCNTG = "longRrtnPcntg";
    private final String KEY_IS_ATLEAST_ONE_LONGRUNNING = "isAtleastOneLongRunning";
    private final String KEY_MIN_LONGRUNNING_CMD_TIMESPAN = "minLngRnCmdTimSpn";
    private final String KEY_MAX_LONGRUNNING_CMD_TIMESPAN = "maxLngRnCmdTimSpn";
    private final String KEY_MIN_SHRT_CMD_TIMESPAN = "minShrtCmdTimeSpn";
    private final String KEY_MAX_SHRT_CMD_TIMESPAN = "maxShrtCmdTimeSpn";
    private final String KEY_DEV_FAILURE_RATIO = "devFailureRatio";
    private final String KEY_ATLEAST_ONE_DEV_FAIL = "atleastOneDevFail";
    private final String KEY_MUST_CMD_PERCENTAGE = "mustCmdPercentage";
    private final String KEY_SIMULATION_START_TIME = "SIMULATION_START_TIME";
    private final String KEY_MAX_DATAPOINT_COLLECTON_SIZE = "MAX_DATAPOINT_COLLECTON_SIZE";
    private final String KEY_RANDOM_SEED = "RANDOM_SEED";
    private final String KEY_MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = "MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING";
    private final String KEY_DATA_STORAE_DIRECTORY = "dataStorageDirectory";
    ///////////////////////////////////////////////////////////////////////////////
    public static boolean IS_RUNNING_BENCHMARK;// = false; // Careful... if it is TRUE, all other parameters will be in don't care mode!

    public static int totalSampleCount;// = 1000;//7500;//10000; // 100000;

    public static boolean IS_PRE_LEASE_ALLOWED;// = true;
    public static boolean IS_POST_LEASE_ALLOWED;// = true;

    public static double shrinkFactor;// = 0.25; // shrink the total time... this parameter controls the concurrency

    public static double minCmdCntPerRtn;// = 1;
    public static double maxCmdCntPerRtn;// = 3;

    public static double zipF;// = 0.01;
    public static int devRegisteredOutOf65Dev;//

    public static int maxConcurrentRtn;// = 100; //in current version totalConcurrentRtn = maxConcurrentRtn;

    public static double longRrtnPcntg;// = 0.1;
    public static  boolean isAtleastOneLongRunning;// = false;
    public static double minLngRnCmdTimSpn;// = 2000;
    public static double maxLngRnCmdTimSpn;// = minLngRnCmdTimSpn * 2;

    public static double minShrtCmdTimeSpn;// = 10;
    public static double maxShrtCmdTimeSpn;// = minShrtCmdTimeSpn * 6;

    public static double devFailureRatio;// = 0.0;
    public static boolean atleastOneDevFail;// = false;
    public static double mustCmdPercentage;// = 1.0;


    public static int SIMULATION_START_TIME;// = 0;
    public static int MAX_DATAPOINT_COLLECTON_SIZE;// = 5000;
    public static int RANDOM_SEED;// = -1;
    public static int MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING;// = 5;

    public static String dataStorageDirectory;// = "C:\\Users\\shegufta\\Desktop\\smartHomeData";
    ////////////////////////////////////////////////////////////////////////////////////



    private static SysParamSngltn singleton;

    public static synchronized SysParamSngltn getInstance()
    {
        if(null == SysParamSngltn.singleton)
        {
            SysParamSngltn.singleton = new SysParamSngltn();
        }

        return SysParamSngltn.singleton;
    }

    private SysParamSngltn()
    {
        Properties properties = new Properties();

        try
        {
            properties.load(new FileInputStream(PROPERTY_FILE_NAME));

            IS_RUNNING_BENCHMARK = Boolean.valueOf(properties.getProperty(KEY_IS_RUNNING_BENCHMARK));
            totalSampleCount = Integer.valueOf(properties.getProperty(KEY_TOTAL_SAMPLE_COUNT));

            shrinkFactor = Double.valueOf(properties.getProperty(KEY_SHRINK_FACTOR));
            minCmdCntPerRtn = Double.valueOf(properties.getProperty(KEY_MIN_CMD_CNT_PER_RTN));
            maxCmdCntPerRtn = Double.valueOf(properties.getProperty(KEY_MAX_CMD_CNT_PER_RTN));

            zipF = Double.valueOf(properties.getProperty(KEY_ZIPF));
            devRegisteredOutOf65Dev = Integer.valueOf(properties.getProperty(KEY_DEV_REGISTERED_OUT_OF_65_DEV));
            maxConcurrentRtn = Integer.valueOf(properties.getProperty(KEY_MAX_CONCURRENT_RTN));

            longRrtnPcntg = Double.valueOf(properties.getProperty(KEY_LONG_RTN_PRCNTG));
            isAtleastOneLongRunning = Boolean.valueOf(properties.getProperty(KEY_IS_ATLEAST_ONE_LONGRUNNING));
            minLngRnCmdTimSpn = Double.valueOf(properties.getProperty(KEY_MIN_LONGRUNNING_CMD_TIMESPAN));
            maxLngRnCmdTimSpn = Double.valueOf(properties.getProperty(KEY_MAX_LONGRUNNING_CMD_TIMESPAN));

            minShrtCmdTimeSpn = Double.valueOf(properties.getProperty(KEY_MIN_SHRT_CMD_TIMESPAN));
            maxShrtCmdTimeSpn = Double.valueOf(properties.getProperty(KEY_MAX_SHRT_CMD_TIMESPAN));

            devFailureRatio = Double.valueOf(properties.getProperty(KEY_DEV_FAILURE_RATIO));
            atleastOneDevFail = Boolean.valueOf(properties.getProperty(KEY_ATLEAST_ONE_DEV_FAIL));
            mustCmdPercentage = Double.valueOf(properties.getProperty(KEY_MUST_CMD_PERCENTAGE));

            IS_PRE_LEASE_ALLOWED = Boolean.valueOf(properties.getProperty(KEY_IS_PRE_LEASE_ALLOWED));
            IS_POST_LEASE_ALLOWED = Boolean.valueOf(properties.getProperty(KEY_IS_POST_LEASE_ALLOWED));

            SIMULATION_START_TIME = Integer.valueOf(properties.getProperty(KEY_SIMULATION_START_TIME));
            MAX_DATAPOINT_COLLECTON_SIZE = Integer.valueOf(properties.getProperty(KEY_MAX_DATAPOINT_COLLECTON_SIZE));
            RANDOM_SEED = Integer.valueOf(properties.getProperty(KEY_RANDOM_SEED));
            MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING = Integer.valueOf(properties.getProperty(KEY_MINIMUM_CONCURRENCY_LEVEL_FOR_BENCHMARKING));

            dataStorageDirectory = getOSindependentPath (properties.getProperty(KEY_DATA_STORAE_DIRECTORY));
        }
        catch(Exception ex)
        {
            System.out.println(ex.toString());
            System.exit(1);
        }
    }

    private static String getOSindependentPath(String filePath)
    {
        String osIndependentPath = filePath;

        int lastIdx = filePath.length() - 1;

        if(filePath.charAt(lastIdx) == '\\' || filePath.charAt(lastIdx) == '/')
        {
            StringBuffer stringBuffer = new StringBuffer(filePath);
            stringBuffer.deleteCharAt(lastIdx);

            osIndependentPath = stringBuffer.toString();
        }

        if(osIndependentPath.contains("/"))
        {
            osIndependentPath = osIndependentPath.replace('/', File.separatorChar);
        }
        else
        {
            osIndependentPath = osIndependentPath.replace('\\', File.separatorChar);
        }

        return osIndependentPath;
    }

    public static void main(String[] args)
    {
        SysParamSngltn.getInstance();
    }
}
