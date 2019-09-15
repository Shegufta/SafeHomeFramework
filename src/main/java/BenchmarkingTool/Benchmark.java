package BenchmarkingTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import Temp.Command;
import Temp.DEV_ID;
import Temp.Routine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * @author Rui Yang
 * @date 07-Sep-2019
 */

public class Benchmark {

  private List<DEV_ID> localDevIdLIst = new ArrayList<>();

  private static int total_num_routines = 0;
  private static List<Routine> routine_list = new ArrayList<>();
  private static double[][] matrix;

  private static final String prePath = "src" + File.separator + "main" + File.separator + "java" + File.separator + "BenchmarkingTool" + File.separator;
  private static final String matrixPath = prePath + "Data" + File.separator + "matrix.tsv";
  private static final String routinePath = prePath  + "Data" + File.separator + "routines.json";

  private void initiateLOCALdevIdList()
  {
    List<DEV_ID> dev_list = Arrays.asList(DEV_ID.values());
    localDevIdLIst.addAll(dev_list);
  }

  public void initiateDevices(List<DEV_ID> globalDevIDlist)
  {
    assert(!localDevIdLIst.isEmpty());

    for(DEV_ID localDevID : this.localDevIdLIst)
    {
      if(!globalDevIDlist.contains(localDevID))
        globalDevIDlist.add(localDevID);
    }
  }

  public Benchmark() throws Exception {
    routine_list = GetRoutineSetFromJson(routinePath);
    System.out.printf("routine list len: %d\n", routine_list.size());
    matrix = GetRoutineMatrix(matrixPath);

    this.initiateLOCALdevIdList();
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    Benchmark benchmark = new Benchmark();
    for (int i = 0; i < 100; ++i) {
      List<Routine> work_load = benchmark.GetOneWorkload(2, -1);
      System.out.println(String.join(",", work_load.toString()));
    }
  }

  public List<Routine> GetOneWorkload(int min_concurrency_level, int rand_seed) throws Exception {

    if(min_concurrency_level > 5)
    {
      System.out.println("\n ERROR: ID 0asdk3 : min_concurrency_level = " + min_concurrency_level + "\n\n.....Terminating...\n");
      System.exit(1);
    }

    Random rand;

    if(rand_seed == -1)
      rand = new Random();
    else
      rand = new Random(rand_seed);

    List<Routine> workload = new ArrayList<>();

    do {
      workload.clear();
      Set<Integer> chosen_ids = new HashSet<>();
      int seed_routine = rand.nextInt(total_num_routines);
      chosen_ids.add(seed_routine);
      workload.add(routine_list.get(seed_routine));
      Queue<Integer> unseeded_ids = new LinkedList<>();
      unseeded_ids.add(seed_routine);
      while (!unseeded_ids.isEmpty() || workload.size() >= total_num_routines) {
        seed_routine = unseeded_ids.poll();
        for (int i = 0; i < total_num_routines; ++i) {
        // System.out.printf("chosen ids %s, seed_routine: %d, i: %d\n", chosen_ids.toString(), seed_routine, i);
          if (!chosen_ids.contains(i) && Math.random() < matrix[seed_routine][i]) {
            workload.add(routine_list.get(i));
            chosen_ids.add(i);
            unseeded_ids.add(i);
          }
        }
      }
    } while (workload.size() < min_concurrency_level);

    int routine_id = 0;
    int registration_time = 0;

    assert(!workload.isEmpty());

    workload.get(0).registrationTime = registration_time;
    workload.get(0).ID = routine_id++;

    for (int i = 1; i < workload.size(); ++i) {
      Routine lst_rtn = workload.get(i-1);
      workload.get(i).registrationTime = lst_rtn.registrationTime + rand.nextInt((int) lst_rtn.backToBackCmdExecutionWithoutGap);
      workload.get(i).ID = routine_id++;
    }

    return workload;
  }

  private static List<Routine> GetRoutineSetFromJson(String path) {
    List<Routine> routine_list = new ArrayList<>();
    JSONParser parser = new JSONParser();
    Random rand = new Random();
    try {
      JSONArray routine_set = (JSONArray) parser.parse(new FileReader(path));

      for (Object obj : routine_set) {
        JSONObject jsonObject = (JSONObject) obj;
        String abbr = (String) jsonObject.get("Routine Shortcut");
        Routine rtn = new Routine(abbr);
        JSONArray commandList = (JSONArray) jsonObject.get("Routine");

        Iterator<String> iterator = commandList.iterator();
        while (iterator.hasNext()) {
          String[] cmd_info = iterator.next().split(":");
          int duration = 0;
          if (cmd_info.length < 3) {
            duration = rand.nextInt(5) + 1;
          } else {
            duration = Integer.parseInt(cmd_info[2]);
          }
          Command cmd = new Command(DEV_ID.valueOf(cmd_info[0]), duration, true);
          rtn.addCommand(cmd);
          // System.out.println(String.join(",",cmd_info));
        }
        routine_list.add(rtn);
        // System.out.print("\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return routine_list;
  }

  private static double[][] GetRoutineMatrix(String path) throws Exception {
    Scanner scanner = new Scanner(new BufferedReader(new FileReader(path)));
    total_num_routines = scanner.nextInt();
    double [][] matrix = new double[total_num_routines][total_num_routines];
    System.out.printf("total_num_routines = %d\n", total_num_routines);
    while(scanner.hasNextDouble()) {
      for (int i = 0; i < total_num_routines; i++) {
        for (int j = 0; j < total_num_routines; j++) {
          matrix[i][j] = scanner.nextDouble();
        }
      }
    }
    // System.out.println(Arrays.deepToString(matrix));
    return matrix;
  }
}
