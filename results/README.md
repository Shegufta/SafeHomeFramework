# Paper Experiment Result Reproduce Instruction

## General Procedure
For figures in paper, we provided independent configuration file for each setting.
To run one setting, you need to:
- Copy the needed configurations to `conf/SafeHomeFramework.config`
- Run the experiment in Intellij
- Results will be saved as subfolders in this `results/` directory

With data for all corresponding setting collected, run figure generation code to 
get visual representation of the results.

***Note***: the figure generation code are general-purposed code, which contains one
metric for all models in each figure. However, the figures in the paper might contain 
different metrics in one plot and might not cover all models. Thus, the code 
generated figures could show all the trend and values but might not look exactly the 
same with paper figures with respect to axis names etc, as well as those with 
multiple metrics.  

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
SafeHomeFramework. To reproduce deployment result, it also needs to setup TP-Link 
HS103 in the same network with Raspberry Pi. This is done with Kasa app. 

SafeHomePrototype also provides dummy device if there is not enough TP-Link HS103 
devices, in which case, the SafeHomePrototype could also be run on other central 
devices such as laptop. For artifact reproducability check, we will be happy to work 
closely to run this deployment result.

### Morning/Factory/Party Scenario Simulation
Steps:
- Copy contents in `config/EuroSys2021PaperConf/morning.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in 
`results/morning`
- Copy contents in `config/EuroSys2021PaperConf/factory.config` to
`config/SafeHomeFramework.config` and run. Results will be stored in 
`results/factory`
- Copy contents in `config/EuroSys2021PaperConf/party.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in 
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

## Figure 15 (c) CDF of Strech Factor
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyCommand.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in 
`results/varyCommand/`
- In `results` folder, run `python3 gen_cdf.py varyCommand`
- The generated figures are in sub-folders: `results/varyCommand/figure/`. The three 
lines in Figure 15(c) are plotted respectively in:
    - `minCmdCntPerRtn2.0/stretch.png` for C = 2
    - `minCmdCntPerRtn4.0/stretch.png` for C = 4
    - `minCmdCntPerRtn8.0/stretch.png` for C = 8  
    **Note** The y-axis range in the three code-generated figures are from 0.0 - 1.0 
    and the paper figure ranges from 0.7 - 1.0.

## Figure 16 (a) (b) (c) Impact of Routine Size
Steps:
- Experiement results already collected in reproducing Figure 15(c). 
- In `results` folder, run `python3 gen_avg.py varyCommand`
- The result folder will be in `results/varyCommand/figure/overall/`.
    - Figure 16(a) is reproduced as `E2E_RTN_TIME.png`
    - Figure 16(b) is reproduced as `PARALLEL_DELTA.png`
    - Figure 16(c) two temporary incongruence lines is reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in 
    `ORDERR_MISMATCH_BUBBLE.png`
    **Note** The y-axis range in two code-generated figures are different.
      
## Figure 16 (d) Impact of Device Popularity
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyAlpha.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in `results/varyAlpha/`
- In `results` folder, run `python3 gen_avg.py varyAlpha`
- The result folder will be in `results/varyAlpha/figure/overall/`.
    - Figure 16(d) is reproduced as `E2E_RTN_TIME.png`
    - Figure 16(d) in the paper has an errata in its x-axis labels (which we will 
    fix in the camera-ready version): the corrected x-axis ranges from 0.0 to 2.0.

## Figure 17 (a) Impact of Long Running Routine Duration
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyLngDuration.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in 
`results/varyLngDuration/`
- In `results` folder, run `python3 gen_avg.py varyLngDuration`
- The result folder will be in `results/varyLngDuration/figure/overall/`.
    - Figure 17(a) two temporary incongruence lines are reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in 
    `ORDERR_MISMATCH_BUBBLE.png`  
    **Note** The y-axis range in two code-generated figures are different.

## Figure 17 (b) Impact of Percentage of Long Running Routines
Steps:
- Copy contents in `config/EuroSys2021PaperConf/varyLngPctg.config` to 
`config/SafeHomeFramework.config` and run. Results will be stored in 
`results/varyLngPctg/`
- In `results` folder, run `python3 gen_avg.py varyLngPctg`
- The result folder will be in `results/varyLngPctg/figure/overall/`.
    - Figure 17(b) two temporary incongruence lines are reproduced separately in 
    `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png` and the order mismatch line is in
    `ORDERR_MISMATCH_BUBBLE.png`  
    **Note** The y-axis range in two code-generated figures are different.
