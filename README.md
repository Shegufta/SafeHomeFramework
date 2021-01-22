# SafeHomeFramework
SafeHome is a management system that provides atomicity and isolation among 
concurrent routines in a smart environment. SafeHome is intended to run at 
an edge device such as a home hub or an enhanced access point from where it
can control devices. SafeHome does not require additional logic on devices. 
SafeHome proposes new Visibility Models (GSV, PSV, EV, and WV) that trade 
off responsiveness against temporary incongruence of smart home state. It 
also provides a serial order of executed routines taking failures into account.
 
SafeHomeFramework is a simulator that mimic SafeHome routine execution logic 
and could compare the performance of different models. SafehomeFramework 
contains both synthetic parameterized workload and realistic workload that 
generated from real-life scenarios.

- [Environment Setup](#environment-setup)
    * [Setup Steps](#setup-steps)
    * [Setup Configurations](#setup-configurations)
    * [Run](#run)
- [Result Collector](#result-collector)
- [Paper Result Reproduce](#paper-result-reproduce)

## Environment Setup
SafeHomeFramework is a java application and runs easiest with the help of 
Intellij. Thus, the pre-request of deploying SafeHomeFramework requires:
- Intellij (best later than 2020.2)
- Java 1.8

### Setup Steps
- Open IntelliJ and import SafeHome project
- Setup Run/Debug Configurations:
    - Add a new configuration with type *Application*
    - Set main class as *Temp.Temp*
    - Set work directory as *the root directory of the project folder*. Note: 
    not the directory stored for Temp.java.
    - Save and apply

### Setup Configurations
In each run, SafeHomeFramework executes all modes and records different metrics 
for each model. It could also be specified to vary a specific parameter with 
configured values.

To run the experiments, you need to specify correct configurations in 
`conf/SafeHomeFramework.config`. All supported parameters are illustrated as
follows:

- `dataStorageDirectory` decides whether to store the simulation results.
- `IS_RUNNING_BENCHMARK` decides whether SafeHomeFramework will run with 
realistic worklaod. It will run synthetic workload if set as `false`. 
- `totalSampleCount` decides the number of runs under the setting.
   
For more detail settings. To run predefined realistic scenarios:
- `benchmark_setting` sets scenario category. Valid value includes `SCENARIO`,
`SHRUNK_MORNING` and `FACTORY`.
- `SCENARIO` type defines sub-scenarios including `MORNING_CHAOS` and `PARTY`.

Pre-defined scenarios are designed with fixed routines. Thus, the routine 
parameters are not variable during the experiments. But the routine set could 
be modified before the run. The routines are defined in 
`src/main/java/BenchmarkingTool/Data`.
  
To run synthetic workloads, SafeHome needs to define:
- `shrinkFactor` defines the range that routines are triggered with respect to
the total length of all generated routines. For example, if all generated routines
can finish execution in 1000 time unit if executed sequentially. All routines will
be triggered between time unit [0, 500) if `shinkFactor = 0.50`.
- `minCmdCntPerRtn` defines the minimum number of commands for each routine.
- `maxCmdCntPerRtn` defines the maximum number of commands for each routine.
- `zipF` defines the parameter for routine start time pattern, which follows Zipf
distribution. Default as 0.1.
- `devRegisteredOutOf65Dev` defines the number of devices that will be touched
during simulation. Max as 65.
- `maxConcurrentRtn` defines the maximum number of concurrent routine that are 
allowed to run in the system.
- `longRrtnPcntg` decides the percentage of long running routines across all 
generated routines.
- `isAtleastOneLongRunning` decides whether there is at least one long-running 
routine in each run.
- `minLngRnCmdTimSpn` decides the minimum length of long-running command.
- `maxLngRnCmdTimSpn` decides the maximum length of long-running command.
- `minShrtCmdTimeSpn` decides the minimum length of short-running command.
- `maxShrtCmdTimeSpn` decides the maximum length of short-running command
- `devFailureRatio` decides the possibility for a device to fail during each run.
- `atleastOneDevFail` decides whether there is at least device to fail during each 
run.
- `mustCmdPercentage` decides the percentage of must command in each routine. 
Other commands are best-effort command which do not need to guarantee atomicity 
property.
- `IS_PRE_LEASE_ALLOWED` decides whether to use pre-lease mechanism for EV model.
- `IS_POST_LEASE_ALLOWED` decides whether to use post-lease mechnism for EV model.

If you would like to vary a specific setting to collect data of how that impact 
different metrics, SafeHomeFramework supports:
- `isVaryShrinkFactor` 
- `isVaryCommandCntPerRtn`
- `isVaryZipfAlpha`
- `isVaryLongRunningPercent`
- `isVaryLongRunningDuration`
- `isVaryShortRunningDuration`
- `isVaryMustCmdPercentage`
- `isVaryDevFailureRatio`  

Only one of the above parameters could set as `true` at one time with all others 
as `false`. Then you could use below two parameters to define the value range of 
the variable you choose to vary in above list.
- `commaSeprtdVarListString` decides the lower bound of each value (inclusive). 
- `commaSeprtdCorrespondingUpperBoundListString` decides the upper bound of each 
value (inclusive).

For example, if set
```
totalSampleCount = 500
isVaryLongRunningDuration = true
commaSeprtdVarListString = 100, 200, 300, 400, 600, 800, 1000
commaSeprtdCorrespondingUpperBoundListString = 110, 210, 310, 410, 610, 810, 1010
``` 
SafeHomeFramework would run 7 sets of experiments, each set runs for 500 runs.
In first run, the long-running command duration are randomly from 100 to 110.
In second run, the long-running command duration are randomly from 200 to 210.
So on so forth until the last set with 1000 to 1010.

### Run
With Run/Debug configuration setup as [Environment Setup](#setup-steps), click 
*Run* in Intellij to run the code.

## Result Collector
SafeHomeFramework collects and provides some initial results for data analysis.
The results are stored in `dataStorageDirectory` specified in configuration file.

In result directory, there will be `N+1` sub-folders and an Overall.txt, where 
the `N` is the number of variable values specified in `commaSeprtdVarListString`.
There will be one folder `avg` contains the average values of each metric for 
different models. One metric one file. The main metric-to-filename mappings are:
- *End-to-end latency*: `E2E_RTN_TIME.dat`
- *Parallel Level*: `PARALLEL_DELTA.dat`
- *Temporary Incongruence*: `ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat`
- *Order Mismatch*: `ORDERR_MISMATCH_BUBBLE.dat`

The `N` other folders includes the CDF data of each metric under the `N` different 
parameter values. One value one folder. The CDF covers the data of 
`totalSampleCount` runs.

## Paper Result Reproduce
The instruction to reproduce results in the paper is in `result/` folder 
