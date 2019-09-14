import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Shegufta Ahsan
 * @project HistogramExtractor
 * @date 13-Sep-19
 * @time 4:51 PM
 */
public class ExtractCDF
{

    public ExtractCDF(String _filePathStr, String columnHeader) throws Exception
    {
        final Path filePath = Paths.get(_filePathStr);

        System.out.println("-----------------------");
        System.out.println("filePath = " + filePath);
        System.out.println("columnHeader = " + columnHeader);
        System.out.println("-----------------------");

        if(!Files.exists(filePath))
        {
            System.out.println("File does not exist... : " + filePath);
            throw new Exception("File does not exist... : " + filePath);
        }

        Scanner sc = null;
        List<Double> dataList = new ArrayList<>();
        List<Double> frequencyList = new ArrayList<>();

        FileWriter fw = null;

        try
        {
            sc = new Scanner(new BufferedReader(new FileReader(filePath.toString())));

            int columnHeaderIndex = -1;
            int dataIndex = -1;

            if(sc.hasNextLine())
            {
                String[] headerLine = sc.nextLine().trim().split("\t");

                for(int I = 0 ; I < headerLine.length ; I++)
                {
                    String header = headerLine[I];

                    if(0 == header.toLowerCase().compareTo(columnHeader.toLowerCase()))
                    {
                        columnHeaderIndex = I;
                        dataIndex = I - 1;
                        break;
                    }
                }

                if(dataIndex == -1)
                    throw new Exception("Header column : " + columnHeader + " not found...");

                while(sc.hasNextLine())
                {
                    String[] dataLine = sc.nextLine().trim().split("\t");

                    Double data = Double.valueOf(dataLine[dataIndex].trim());
                    Double frequency = Double.valueOf(dataLine[columnHeaderIndex].trim());

                    if(data == -1 || frequency == -1)
                        break;

                    dataList.add(data);
                    frequencyList.add(frequency);
                }

                if(dataList.isEmpty())
                    throw new Exception("no data found...");

                final double frequencySteps = frequencyList.get(0);

                if(0.0 == frequencySteps)
                    throw new Exception("frequency step is zero!");

                //Double totalTime = 1/frequencySteps;

                List<Double> regeneratedList = new ArrayList<>();

                for(int I = 0 ; I < frequencyList.size() ; I++)
                {
                    double data = dataList.get(I);
                    double frequencyLowerBound = frequencyList.get(I);

                    regeneratedList.add(data);

                    if( (I < frequencyList.size()-1) && data == dataList.get(1 + I) )
                    {
                        double frequencyUpperBound = frequencyList.get(++I); // increment I NOTE: increment first, then use,... i.e. ++I

                        do
                        {
                            regeneratedList.add(data);
                            frequencyLowerBound += frequencySteps;
                        }while(frequencyLowerBound <= frequencyUpperBound);
                    }
                }

                String outputFileName = columnHeader + ".dat";
                File file = new File(outputFileName);
                fw = new FileWriter(file);
                fw.write(columnHeader + "\n");

                for(double data : regeneratedList)
                {
                    fw.write(data + "\n");
                }
            }
            else
            {
                throw new Exception("No Header Found....");
            }

        }
        catch(Exception ex)
        {
            throw ex;
        }
        finally
        {
            if(sc != null)
            {
                sc.close();
                sc = null;
            }

            if(fw != null)
            {
                fw.close();
                fw = null;
            }
        }


    }

    public static void main(String[] args) throws Exception
    {
        if(args.length < 2)
        {
            System.out.println("argument1: filePath, argument2: consistencyType");
            System.exit(1);
        }

        String filePathStr = args[0]; //Paths.get("data/ISVLTN1_PER_RTN_COLLISION_COUNT.dat").toString();
        String columnHeader = args[1];//"LAZY_FCFS";

        ExtractCDF extractCDF = new ExtractCDF(filePathStr, columnHeader);


    }
}
