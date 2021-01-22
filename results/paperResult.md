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
The deployment is on Raspberry Pi 4 (prefer 4GB) controlling TP-Link HS103 devices. 
The deployment code is in [SafeHomePrototype repository](https://github.com/Shegufta/SafeHomePrototype). 
The deployment code shares the same logic and same setup process with 
SafeHomeFramework. To reproduce deployment result, it also needs to setup TP-Link 
HS103 in the same network with Raspberry Pi. This is done with Kasa app. 

SafeHomePrototype also provides dummy device if there is not enough TP-Link HS103 
devices, in which case, the SafeHomePrototype could also be run on other central 
devices such as laptop. For artifact reproducability check, we will be happy to work 
closely to run this deloyment result.

### Morning/Factory/Party Scenario Simulation
Steps:
- Copy contents in `config/morning.config` to `config/SafeHomeFramework.config`
  and run. Results will be stored in `results/morning`
- Copy contents in `config/factory.config` to `config/SafeHomeFramework.config`
  and run. Results will be stored in `results/factory`
- Copy contents in `config/party.config` to `config/SafeHomeFramework.config`
  and run. Results will be stored in `results/party`
  
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
