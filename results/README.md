# Paper Experiment Result Reproduce Instruction

This instruction intends to help the reproduction of results from Eurosys 2021 paper.
The metrics collected following below instructions will closely match paper's results 
and claims. This instruction helped to provide plotting scripts to reproduce some 
paper figures e.g. Figure 12, 16, 17, but some other figures need to combine collected 
metric or do some simple post processing to get the numbers in paper. We will include
detailed processing steps in the instruction.

## General Procedure
For figures in paper, we provided independent configuration file for each setting.
To run one setting, you need to:
- Copy the needed configurations to `conf/SafeHomeFramework.config`
- Run the experiment in Intellij
- Results will be saved as subfolders in this `results/` directory

With data for all corresponding setting collected, run figure generation code to 
get visual representation of the results.

***Notes***: 
1. the figure generation code are general-purposed code, which contains one
metric for all models in each figure. However, the figures in the paper might contain 
different metrics in one plot and might not cover all models. Thus, the code 
generated figures could show both the trend and values but might not share the same 
x-axis/y-axis range and unit. Also, it cannot visually combine multiple metrics.  
2. some figures in the paper are generated with manual data processing 
(not through script). For such figures, we will provide all needed configurations 
to generate corresponding metric results. And we'll list how we process them step 
by step to get the figure.
3. Most of the provides configurations run for 1000 times for each data 
point, which may take pretty long time depending on the working machine. The number
of runs could be reduced to smaller number as 100. The data trend will be mostly the
same, but the curve might not be as smooth as in paper and may suffer a little data 
bias. 
4. The provided configurations are supposed to be directly reusable for MacOS and 
Linux. For windows, the dataStorageDirectory always need to be manually modified 
correspondingly to use double backslash `\\` after copying.

## Figure 12 (a) Real-Word Workload
This experiment includes 4 sets of experiments:
- Morning scenario in deployment setting
- Morning scenario in simulator (SafeHomeFramework)
- Party scenario in simulator (SafeHomeFramework)
- Factory scenario in simulator (SafeHomeFramework)

### Morning Scenario Deployment
The deployment is on Raspberry Pi (prefer 4GB) controlling TP-Link HS103 devices. 
The deployment code is in [SafeHomePrototype repository](https://github.com/Shegufta/SafeHomePrototype). 
The deployment code shares the same logic and same setup process with 
SafeHomeFramework. The code with result collection is in *with-measurement* 
branch. To reproduce deployment result, it also needs to setup TP-Link 
HS103 in the same network with Raspberry Pi. This is done with Kasa app. 

SafeHomePrototype also provides dummy device if there is not enough TP-Link HS103 
devices, in which case, the SafeHomePrototype could also be run on other central 
devices such as laptop. For artifact reproducability check, we will be happy to work 
closely to run this deployment result.

### Morning/Factory/Party Scenario Simulation
Steps:
- Copy contents in `config/EuroSys2021PaperConf/morning.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/morning`
- Copy contents in `config/EuroSys2021PaperConf/factory.config` to
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/factory`
- Copy contents in `config/EuroSys2021PaperConf/party.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/party`
  
### Generate Figure 12(a)
Run `python3 gen_fig12.py` under `results/` folder. It will generate 
`results/fig12.png`.

***Note*** the script current takes the result for morning scenarios as deployment
results since deployment result could not be generated through SafeHomeFramework. To
use SafeHomePrototype result, just copy their result folder under this `results/` and
then run `python3 gen_fig12.py -d [DEPLOYMENT_RESULT_FOLDER]`.

## Figure 12(b) Final Incongruence
The final incongruence is evaluated through end-state simulation. From this `results/`
folder:
```
cd finalIncong/
python3 end-state-simulator.py
python3 gen-es-bar.py
```
This will generate Figure 12(b) named `end-state.png` in your current folder 
`results/finalIncong/`.

## Figure 13 (a) (c) Impact of Must Command Under Failure
Steps:
- Copy contents in `config/EuroSys2021PaperConf/fig13ac.config` to 
  `config/SafeHomeFramework.config` and run ***SimulateFailure***. Results will be 
  stored in `results/fig13ac/[folder_with_timestamp_in_name]`
- In `results` folder, run `python3 gen_avg.py -d fig13ac`
- The generated figures are in `results/fig13ac/[folder_with_timestamp_in_name]/figure/overall/`
    - `ABORT_RATE.png` is reproduced as Figure 13(a)
    - `RECOVERY_CMD_PER_RTN.png` is reproduced for Figure 13(c) with an unprocessed 
    y-axis. To get data in Figure 13(c), the reproduced result needs to be multiplied
    by `100.0/#cmdCntPerRtn`. `#cmdCntPerRtn=4` in this setting. Thus, the reproduced 
    value should multiply 25 for result (which will match Figure 13(c)). 
    
## Figure 13 (b) (d) Impact of Must Command Under Failure
Steps:
- Copy contents in `config/EuroSys2021PaperConf/fig13bd.config` to 
  `config/SafeHomeFramework.config` and run ***SimulateFailure***. Results will be 
  stored in `results/fig13bd/[folder_with_timestamp_in_name]`
- In `results` folder, run `python3 gen_avg.py -d fig13bd`
- The generated figures are in `results/fig13bd/[folder_with_timestamp_in_name]/figure/overall/`
    - `ABORT_RATE.png` is reproduced as Figure 13(b)
    - `RECOVERY_CMD_PER_RTN.png` is reproduced for Figure 13(d) with an unprocessed 
    y-axis. To get data in Figure 13(d), the reproduced result needs to be multiplied
    by `100.0/#cmdCntPerRtn`. `#cmdCntPerRtn=4` in this setting. Thus, the reproduced 
    value should multiply 25 for result (which will match Figure 13(d)).  

## Figure 14 Scheduling Polices
Steps:
- Go to `src/main/java/SafeHomeSimulator/SafeHomeSimulator.java`. Modify as 
`isGeneratingFigure14 = true` (around line 267)
- Copy contents in `config/EuroSys2021PaperConf/fig14.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/fig14/`
- In `results` folder, run `python3 gen_bar_charts.py -d fig14`
- The generated figures in folder `results/fig14/figure/overall`:
   - Among all three generated figures the legend number is the reciprocal for legend in 
   Figure 14. For example, $\rou=8$ is shown with 0.125 in code-generated figure. 
   - Figure 14(a) is reproduced as `E2E_RTN_TIME.png`
   - Figure 14(b) is reproduced as `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png`
   - Figure 14(c) is reproduced as `PARALLEL_DELTA.png`

## Figure 15 (c) CDF of Strech Factor
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyCommand.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/varyCommand/`
- In `results` folder, run `python3 gen_cdf.py -d varyCommand`
- The generated figures are in sub-folders: `results/varyCommand/figure/`. The three 
lines in Figure 15(c) are plotted respectively in:
    - `minCmdCntPerRtn2.0/stretch.png` for C = 2
    - `minCmdCntPerRtn4.0/stretch.png` for C = 4
    - `minCmdCntPerRtn8.0/stretch.png` for C = 8  
    **Note** The y-axis range in the three code-generated figures are from 0.0 - 1.0 
    and the paper figure ranges from 0.7 - 1.0.

## Figure 15 (d) Routine Scheduling Overhead
Steps:
- Copy contents in `config/EuroSys2021PaperConf/fig15d.config` to 
  `config/SafeHomeFramework.config` and run ***SimulateFailure***. Results will be 
  stored in `results/fig15d/[folder_with_timestamp_in_name]`
- In `results` folder, run `python3 gen_avg.py -d fig15d/[folder_with_timestamp_in_name]`
- The generated figures are in `results/fig15d/[folder_with_timestamp_in_name]/figure/overall/`
    - Figure 15(d) is reproduced as `EV_ROUTINE_INSERT_TIME_MICRO_SEC.png`. 

## Figure 16 (a) (b) (c) Impact of Routine Size
Steps:
- Experiement results already collected in reproducing Figure 15(c). 
- In `results` folder, run `python3 gen_avg.py -d varyCommand`
- The result folder will be in `results/varyCommand/figure/overall/`.
    - Figure 16(a) is reproduced as `E2E_RTN_TIME.png`. Y-axis uses seconds as unit 
    while the paper uses minutes.
    - Figure 16(b) is reproduced as `PARALLEL_DELTA.png`
    - Figure 16(c) two temporary incongruence lines is reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in 
    `ORDERR_MISMATCH_BUBBLE.png`
    **Note** The y-axis range in two code-generated figures are different.
      
## Figure 16 (d) Impact of Device Popularity
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyAlpha.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/varyAlpha/`.
- In `results` folder, run `python3 gen_avg.py -d varyAlpha`.
- The result folder will be in `results/varyAlpha/figure/overall/`.
    - Figure 16(d) is reproduced as `E2E_RTN_TIME.png`. Y-axis uses seconds as unit 
    while the paper uses minutes.
    - Figure 16(d) in the paper has an errata in its x-axis labels (which we will 
    fix in the camera-ready version): the corrected x-axis ranges from 0.0 to 2.0.

## Figure 17 (a) Impact of Long Running Routine Duration
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyLngDuration.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator***. Results will be stored in 
`results/varyLngDuration/`
- In `results` folder, run `python3 gen_avg.py -d varyLngDuration`
- The result folder will be in `results/varyLngDuration/figure/overall/`.
    - Figure 17(a) two temporary incongruence lines are reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in 
    `ORDERR_MISMATCH_BUBBLE.png`  
    **Note** The y-axis range in two code-generated figures are different.

## Figure 17 (b) Impact of Percentage of Long Running Routines
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyLngPctg.config` to 
`config/SafeHomeFramework.config` and run ***SafeHomeSimulator*** Results will be stored in 
`results/varyLngPctg/`
- In `results` folder, run `python3 gen_avg.py -d varyLngPctg`
- The result folder will be in `results/varyLngPctg/figure/overall/`.
    - Figure 17(b) two temporary incongruence lines are reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in
    `ORDERR_MISMATCH_BUBBLE.png`  
    **Note** The y-axis range in two code-generated figures are different.
