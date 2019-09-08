package Temp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * @author Rui Yang
 * @date 07-Sep-2019
 */

public class Benchmark {
  private static int total_num_routines = 0;
  private static List<Routine> routine_list = new ArrayList<>();
  private static double[][] matrix;

  public Benchmark() throws Exception {
    routine_list = GetRoutineSetFromJson("data/routines.json");
    System.out.printf("routine list len: %d\n", routine_list.size());
    matrix = GetRoutineMatrix("data/matrix.tsv");
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    routine_list = GetRoutineSetFromJson("data/routines.json");
    System.out.printf("routine list len: %d\n", routine_list.size());
    matrix = GetRoutineMatrix("data/matrix.tsv");
    for (int i = 0; i < 100; ++i) {
      List<Routine> work_load = GetOneWorkload(2);
      System.out.println(String.join(",", work_load.toString()));
    }
  }

  public static List<Routine> GetOneWorkload(int min_concurrency_level) throws Exception {
    Random rand = new Random();
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
