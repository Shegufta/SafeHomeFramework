package BenchmarkingTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

  public enum SETTING_TYPE {
    MATRIX,
    SCENARIO,
    FACTORY
  }

  public enum SCENARIO {
    MORNING,
    MORNING_CHAOS,
    PARTY,
    SHORT
  }

  public enum FACTORY {
    SIMPLE_CHAOS,
    PIPELINE_WITHCOMMON_DEV,
    SYNTHETIC
  }

  private SETTING_TYPE setting_type = SETTING_TYPE.FACTORY;

  // Setting for scenarios
  private SCENARIO scenario = SCENARIO.MORNING_CHAOS;
  private boolean is_fixed_fst_lst_interval = true;
  private int fst_lst_rtn_interval = 900;
  private boolean is_first_routine_fixed = true;
  private boolean is_last_routine_fixed = true;

  // Setting for factory
  private FACTORY fac_type = FACTORY.SYNTHETIC;
  private static int num_synthetic_group = 15; // max 100
  private static int num_dev_per_group = 4; // max 4
  private static double local_dev_touching_rate = 0.6;
  private static int num_cmn_per_neighbor = 4; // max 4
  private static double cmn_dev_touching_rate = 0.3;
  private static int num_global_dev = 5; // max 10
  private static double glb_dev_touching_rate = 0.6;
  private static int max_time_per_dev = 30;
  private static int num_item = 8;

  private boolean random_dev = true;
  private static int local_rtn_per_stage = 6;
  private static int common_rtn_per_neighbor = 3;
  private static int global_rtn = 5;
  private static int intv_between_stage = 5;

  private double SHT_CMD_RTN_MULTI = 10.0;

  private List<DEV_ID> localDevIdLIst = new ArrayList<>();

  private static int total_num_routines = 0;
  private static List<Routine> routine_list = new ArrayList<>();
  private static List<Routine> sht_cmd_rtn_list = new ArrayList<>();
  private static double[][] matrix;

  private static final String prePath = "src" + File.separator + "main" + File.separator + "java" + File.separator + "BenchmarkingTool" + File.separator;
  private static final String matrixPath = prePath + "Data" + File.separator + "matrix.tsv";
  private static final String routinePath = prePath  + "Data" + File.separator + "routines.json";
  private static String scnRoutinePath = prePath + "Data" + File.separator + "scn1.json";
  private static String facRoutinePath = prePath + "Data" + File.separator + "scn-fac-simple.json";
  private static String facLocalRoutinePath =  prePath + "Data" + File.separator + "scn-fac-local.json";
  private static String facCommonRoutinePath =  prePath + "Data" + File.separator + "scn-fac-common.json";
  private static String facGlobalRoutinePath =  prePath + "Data" + File.separator + "scn-fac-global.json";


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

  private Random rand;
  private int min_concurrency_level;

  public Benchmark(int rand_seed, int _min_concurrency_level) throws Exception {

    if(min_concurrency_level > 5)
    {
      System.out.println("\n ERROR: ID 0asdk3 : min_concurrency_level = " + min_concurrency_level + "\n\n.....Terminating...\n");
      System.exit(1);
    }

    this.min_concurrency_level = _min_concurrency_level;

    if(rand_seed < 0)
      this.rand = new Random();
    else
      this.rand = new Random(rand_seed);

    if (setting_type == SETTING_TYPE.MATRIX) {
      routine_list = GetRoutineSetFromJson(routinePath);
      sht_cmd_rtn_list = GetShortCommandRoutineList();  // one per device;
      System.out.printf("routine list len: %d\n", routine_list.size());
      matrix = GetRoutineMatrix(matrixPath);
    } else if (setting_type == SETTING_TYPE.SCENARIO) {
      if (scenario == SCENARIO.PARTY) {
        scnRoutinePath = prePath + "Data" + File.separator + "scn2.json";
        is_fixed_fst_lst_interval = false;
        fst_lst_rtn_interval = 7200;
        is_first_routine_fixed = true;
        is_last_routine_fixed = false;
      } else if (scenario == SCENARIO.MORNING) {
        scnRoutinePath = prePath + "Data" + File.separator + "scn1.json";
        is_fixed_fst_lst_interval = true;
        fst_lst_rtn_interval = 900;
        is_first_routine_fixed = true;
        is_last_routine_fixed = true;
      } else if (scenario == SCENARIO.MORNING_CHAOS) {
        scnRoutinePath = prePath + "Data" + File.separator + "scn-chaos-morning2.json";
        is_fixed_fst_lst_interval = true;
        fst_lst_rtn_interval = 1500;
        is_first_routine_fixed = false;
        is_last_routine_fixed = true;
      } else if (scenario == SCENARIO.SHORT) {
        scnRoutinePath = prePath + "Data" + File.separator + "scn3.json";
        is_fixed_fst_lst_interval = false;
        fst_lst_rtn_interval = 60;
        is_first_routine_fixed = true;
        is_last_routine_fixed = false;
      }
      routine_list = GetRoutineSetFromJson(scnRoutinePath);
      System.out.printf("routine list len: %d\n", routine_list.size());
    } else { // factory mode
      fst_lst_rtn_interval = 180;
      routine_list = GetRoutineSetFromJson(facRoutinePath);
      System.out.printf("routine list len: %d\n", routine_list.size());
    }
    this.initiateLOCALdevIdList();
  }


  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    Benchmark benchmark = new Benchmark(-1, 4);
    for (int i = 0; i < 100; ++i) {
      List<Routine> work_load = benchmark.GetOneWorkload();
      System.out.println(String.join(",", work_load.toString()));
    }
  }

  public List<Routine> GetOneWorkload() throws Exception {

    if (setting_type == SETTING_TYPE.SCENARIO) {
      return GetOneScnWorkload();
    } else if (setting_type == SETTING_TYPE.FACTORY) {
      return GetOneFacWorkload();
    }

    List<Routine> workload = new ArrayList<>();
    List<Routine> final_workload = new ArrayList<>();
    do {
      workload.clear();
      Set<Integer> chosen_ids = new HashSet<>();
      int seed_routine = this.rand.nextInt(total_num_routines);
      chosen_ids.add(seed_routine);
      workload.add(routine_list.get(seed_routine));
      Queue<Integer> unseeded_ids = new LinkedList<>();
      unseeded_ids.add(seed_routine);
      while (!unseeded_ids.isEmpty() || workload.size() >= total_num_routines) {
        seed_routine = unseeded_ids.poll();
        for (int i = 0; i < total_num_routines; ++i) {
        // System.out.printf("chosen ids %s, seed_routine: %d, i: %d\n", chosen_ids.toString(), seed_routine, i);
          if (!chosen_ids.contains(i) && this.rand.nextDouble()< matrix[seed_routine][i]) {
            workload.add(routine_list.get(i));
            chosen_ids.add(i);
            unseeded_ids.add(i);
          }
        }
      }
    } while (workload.size() < min_concurrency_level);

    int registration_time = 0;

    assert(!workload.isEmpty());

    workload.get(0).registrationTime = registration_time;

    for (int i = 1; i < workload.size(); ++i) {
      Routine lst_rtn = workload.get(i - 1);
      workload.get(i).registrationTime = lst_rtn.registrationTime + this.rand.nextInt((int) lst_rtn.backToBackCmdExecutionWithoutGap);
    }

    int routine_id = 0;
    final_workload.add(workload.get(0));
    final_workload.get(0).ID = routine_id++;

    Set<Integer> chosen_sht_rtn_ids = new HashSet<>();
    for (int i = 1; i < workload.size(); ++i) {
      Routine lst_workload = workload.get(i - 1);
      int interval = workload.get(i).registrationTime - lst_workload.registrationTime;
      int last_time = lst_workload.registrationTime;
      double sht_cmd_rtn_prob = interval * 1.0 / SHT_CMD_RTN_MULTI;
      while (sht_cmd_rtn_prob > 0 && this.rand.nextDouble() < sht_cmd_rtn_prob) {
        if (chosen_sht_rtn_ids.size() == sht_cmd_rtn_list.size()) {
          break;
        }
        int id;
        do {
          id = this.rand.nextInt(sht_cmd_rtn_list.size());
        } while (chosen_sht_rtn_ids.contains(id));
        chosen_sht_rtn_ids.add(id);
        final_workload.add(sht_cmd_rtn_list.get(id));
        int bound = (workload.get(i).registrationTime - last_time);
        final_workload.get(routine_id).registrationTime = bound > 0 ? last_time + this.rand.nextInt(bound) : last_time;
        last_time = final_workload.get(routine_id).registrationTime;
        final_workload.get(routine_id).ID = routine_id++;
        sht_cmd_rtn_prob--;
      }
      final_workload.add(workload.get(i));
      final_workload.get(routine_id).ID = routine_id++;
    }
    return final_workload;
  }

  private List<Routine> GetOneFacWorkload() {
    // Add all routines into workload

    if (fac_type == FACTORY.SIMPLE_CHAOS) {
      List<Routine> workload = new ArrayList<>(routine_list);
      // Shuffle list
      Collections.shuffle(workload);
      assert(!workload.isEmpty());

      // Assign routine ID (for sequence)
      for (int i = 0; i < workload.size(); ++i) {
        workload.get(i).ID = i;
      }

      // Get a list of random registration time, sort, and assign
      List<Integer> reg_times = IntStream.range(0, workload.size() - 2)
          .mapToObj(i -> ThreadLocalRandom.current().nextInt(0, fst_lst_rtn_interval + 1))
          .sorted()
          .collect(Collectors.toList());

      workload.get(0).registrationTime = 0;
      workload.get(workload.size() - 1).registrationTime = is_fixed_fst_lst_interval ? fst_lst_rtn_interval: reg_times.get(reg_times.size() - 1);

      for (int i = 1; i < workload.size() - 1; ++i) {
        workload.get(i).registrationTime = reg_times.get(i - 1);
      }
      return workload;
    } else if (fac_type == FACTORY.PIPELINE_WITHCOMMON_DEV) {
      List<Routine> local_rtns = GetRoutineSetFromJson(facLocalRoutinePath);
      local_rtns.sort(Comparator.comparing(obj -> obj.abbr));

      int local_interval = fst_lst_rtn_interval / local_rtn_per_stage;
      int num_stage = local_rtns.size() / local_rtn_per_stage;
      System.out.println(num_stage);
      for (int i = 0; i < local_rtns.size(); ++i) {
        local_rtns.get(i).registrationTime = (i % local_rtn_per_stage) * local_interval  + (i / local_rtn_per_stage) * intv_between_stage;
      }

//      System.out.printf("Local Routines: %s \n", local_rtns.toString());


      // Fetch common and global routines and assign time
      List<Routine> common_glb_rtns = GetRoutineSetFromJson(facCommonRoutinePath);
      common_glb_rtns.addAll(GetRoutineSetFromJson(facGlobalRoutinePath));
      // Get a list of random registration time and assign
      List<Integer> reg_times = IntStream.range(0, common_glb_rtns.size() - 1)
          .mapToObj(i -> ThreadLocalRandom.current().nextInt(0, fst_lst_rtn_interval + 1))
          .collect(Collectors.toList());
      for (int i = 1; i < common_glb_rtns.size() - 1; ++i) {
        common_glb_rtns.get(i).registrationTime = reg_times.get(i - 1);
      }

      // Combine local and common rtns, sort and assign routine ID.
      List<Routine> workload = new ArrayList<>(local_rtns);
      workload.addAll(common_glb_rtns);
      workload.sort(Comparator.comparingInt(r -> r.registrationTime));
      for (int i = 0; i < workload.size(); ++i) {
        workload.get(i).ID = i;
      }

      return workload;
    } else {
      return getFactorySyntheticWorkload();
    }

  }

  private List<Routine> getFactorySyntheticWorkload() {

    List<Routine> workload = new ArrayList<>();
    // Assume each group has enough unfinished items to process
//    HashSet<Integer> existing_start_time = new HashSet<Integer>();
    do {
      for (int group = 0; group < num_synthetic_group; ++group) {
        int lst_cmd_finish_time = this.rand.nextInt(num_synthetic_group * 2);
        for (int i = 0; i < num_item; ++i) {
          Routine rtn = new Routine("LC_" + Integer.toString(group) + "_" + Integer.toString(i));
          rtn.registrationTime = lst_cmd_finish_time;

          if (group > 0) {
            for (int cmn_dev = 0; cmn_dev < num_cmn_per_neighbor; ++cmn_dev) {
              if (this.rand.nextDouble() < cmn_dev_touching_rate) { // device is touched for this item
                int duration = this.rand.nextInt(max_time_per_dev) + 1;
                Command cmd = new Command(
                    DEV_ID.valueOf("common_dev_" + Integer.toString(group - 1) + "_" + Integer.toString(cmn_dev)), duration, true);
                lst_cmd_finish_time += duration;
                rtn.addCommand((cmd));
              }
            }
          }
          for (int local_dev = 0; local_dev < num_dev_per_group; ++local_dev) {
            if (this.rand.nextDouble() < local_dev_touching_rate) { // device is touched for this item
              int duration = this.rand.nextInt(max_time_per_dev) + 1;
              Command cmd = new Command(
                  DEV_ID.valueOf("local_dev_" + Integer.toString(group) + "_" + Integer.toString(local_dev)), duration, true);
              lst_cmd_finish_time += duration;
              rtn.addCommand((cmd));
            }
          }
          for (int cmn_dev = 0; cmn_dev < num_cmn_per_neighbor; ++cmn_dev) {
            if (this.rand.nextDouble() < cmn_dev_touching_rate) { // device is touched for this item
              int duration = this.rand.nextInt(max_time_per_dev) + 1;
              Command cmd = new Command(
                  DEV_ID.valueOf("common_dev_" + Integer.toString(group) + "_" + Integer.toString(cmn_dev)), duration, true);
              lst_cmd_finish_time += duration;
              rtn.addCommand((cmd));
            }
          }
          for (int glb_dev = 0; glb_dev < num_global_dev; ++glb_dev) {
            if (this.rand.nextDouble() < glb_dev_touching_rate) { // device is touched for this item
              int duration = this.rand.nextInt(max_time_per_dev) + 1;
              Command cmd = new Command(
                  DEV_ID.valueOf("global_dev_" + Integer.toString(glb_dev)), duration, true);
              lst_cmd_finish_time += duration;
              rtn.addCommand((cmd));
            }
          }
          if (rtn.isEmpty()) { continue; }
          workload.add(rtn);
        }
      }

      // Sort the workload by registration time and assign ID
      workload.sort(Comparator.comparingInt(r -> r.registrationTime));
//    // modify duplicate time
//    for (int i = 0; i < workload.size(); ++i) {
//    }
      for (int i = 0; i < workload.size(); ++i) {
        workload.get(i).ID = i;
      }
    } while (workload.size() == 0);

    return workload;

  }

  private List<Routine> GetOneScnWorkload() {
    // Add all routines into workload
    List<Routine> workload = new ArrayList<>(routine_list);

    // Shuffle list
    Collections.shuffle(workload.subList(is_first_routine_fixed ? 1 : 0, is_last_routine_fixed ? workload.size() - 1: workload.size()));
    assert(!workload.isEmpty());

    // Assign routine ID (for sequence)
    for (int i = 0; i < workload.size(); ++i) {
      workload.get(i).ID = i;
    }

    // Get a list of random registration time, sort, and assign
    List<Integer> reg_times = IntStream.range(0, workload.size() - 2)
        .mapToObj(i -> ThreadLocalRandom.current().nextInt(0, fst_lst_rtn_interval + 1))
        .sorted()
        .collect(Collectors.toList());

    workload.get(0).registrationTime = 0;
    workload.get(workload.size() - 1).registrationTime = is_fixed_fst_lst_interval ? fst_lst_rtn_interval: reg_times.get(reg_times.size() - 1);

    for (int i = 1; i < workload.size() - 1; ++i) {
      workload.get(i).registrationTime = reg_times.get(i - 1);
    }

    return workload;
  }

  private List<Routine> GetShortCommandRoutineList() {
    List<Routine> routine_list = new ArrayList<>();
    List<DEV_ID> dev_list = Arrays.asList(DEV_ID.values());
    for (DEV_ID dev: dev_list) {
      if (dev.compareTo(DEV_ID.Z) <= 0) {
        continue;
      }
      int duration = this.rand.nextInt(5) + 1;
      Command cmd = new Command(dev, duration, true);
      Routine rtn = new Routine(dev.name());
      rtn.addCommand(cmd);
      routine_list.add(rtn);
    }
    return routine_list;
  }

  private List<Routine> GetRoutineSetFromJson(String path) {
    List<Routine> routine_list = new ArrayList<>();
    JSONParser parser = new JSONParser();

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
            duration = this.rand.nextInt(5) + 1;
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
          matrix[i][j] = Math.min(scanner.nextDouble() * 1.5, 1.0);
        }
      }
    }
    // System.out.println(Arrays.deepToString(matrix));
    return matrix;
  }
}
