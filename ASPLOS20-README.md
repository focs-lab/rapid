## Overview

We presented an implementation for the algorithm presented in our ASPLOS'20 paper.
The algorithm (Aerodrome) analyzes executions of concurrent programs and detects violations of conflict serializability.
We also compare against the former state-of-the-art algorithm Velodrome.

## Requirements

1. Machine with Unix based operating system, such as Ubuntu or MacOS.
2. Java 1.8 
3. Apache Ant
4. Python 2.7 or higher

## Overall Workflow

Our benchmarks are provided in the directory `benchmarks/`.
The overall workflow is pretty simple - 

1. We first need to generate execution logs. 
We will use [RoadRunner](https://github.com/stephenfreund/RoadRunner) for this.
For each benchmark, we will generate a file called `full_trace.rr` using RoadRunner.

2. We will then account for externally provided atomicity specifications. 
For this we basically need to modify these execution logs by removing certain events.
The directory `atomicity_specs/` contains a `.txt` file for each benchmark.
This file contains hints for which events to be removed from `full_trace.rr` corresponding to the benchmark. 
The resulting execution log will be called `trace.rr`.

3. We then analyze the modified execution logs `trace.rr`.
For this, we need our tool [RAPID](https://github.com/umangm/rapid/).
Rapid can give several kinds of information about an execution log:
	- The class `MetaInfo` in RAPID can be used to determine basic information about the log, including the total number of events, threads, variables, locks etc.
	- The 'Aerodrome' in RAPID determines conflict serializability violations using our proposed algorithm Aerodrome.
	- The 'Velodrome' in RAPID determines conflict serializability violations using the prior state-of-the-art algorithm Velodrome.

**Remark** - 
In the following we will assume that the directory in which this file is located is stored in the environment variables $AE_HOME.
For example, if your directory is `/path/to/asplos-ae/`, then you would execute:
```
export AE_HOME=/path/to/asplos-ae
```
Also, you need to change the variable `home` in the file `scripts/util.py` (line 17) to be the value of $AE_HOME .

## Generating traces and Accounting for Atomicity Specifications

### Download and install Roadrunner : 
```
cd $AE_HOME
git clone https://github.com/stephenfreund/RoadRunner
cd $AE_HOME/RoadRunner
ant
source msetup
```

### Extract execution logs
** This step will take up a lot of time **

If you want to generate full trace for a single benchmark, then you should run :
```
python gen_trace.py <benchmark_name>
```
Here, `<benchmark_name>` could be something like `philo`.

Alternatively, you could generate traces for all benchmarks as:
```
python gen_trace.py
```

This step generates files `$AE_HOME/benchmarks/<benchmark_name>/full_trace.rr`, either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran.


### Account for atomicity specifications
** This step will take up a lot of time **

If you want to modify the trace for a single benchmark:
```
python atom_spec.py <benchmark_name>
```
Here, `<benchmark_name>` could be something like `philo`.

Alternatively, you could generate traces for all benchmarks as:
```
python atom_spec.py
```

This step generates files `$AE_HOME/benchmarks/<benchmark_name>/trace.rr`, either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran.

At this point, the files `$AE_HOME/benchmarks/<benchmark_name>/full_trace.rr` are redundant. You may want to delete them.

## Getting Trace metadata

If you want to get the metadata about the trace of a single benchmark:
```
python metainfo.py <benchmark_name>
```
Here, `<benchmark_name>` could be something like `philo`.

Alternatively, if you have generated `trace.rr` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python metainfo.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<benchmark_name>/metainfo.txt`
	- `$AE_HOME/benchmarks/<benchmark_name>/metainfo.err`
	- `$AE_HOME/benchmarks/<benchmark_name>/metainfo.tim`

The file `metainfo.err` should ideally be empty. If it is not empty, it contains error information from the Java command run in the python script `metainfo.py`.

The file `metainfo.txt` contains the actual output (including the number of different kinds of events).
An example is below:
```
Number of locations = 3046
Number of threads = 7
Number of locks = 7
Number of variables = 1079526
Number of variables (read) = 744962
Number of variables (write) = 1054927

Number of events = 2434653788
Number of read events = 1006151943
Number of write events = 427577038
Number of reads+writes = 1433728981
Number of acquire events = 1470277
Number of release events = 1470277
Number of fork events = 6
Number of join events = 6
Number of begin events = 498992156
Number of end events = 498992085
Number of branch events = 0
```
The 1st line above describes the lines of code touched by the execution.
The 2nd, 3rd and 4th lines describe the number of threads, locks and variables in the execution.
The 5th and 6th line describe the number of variables that were read from and written to.
The later lines describe the number of events (total and of different kinds).

The file `metainfo.tim` reports the time taken.

## Running Aerodrome

If you want to analyze the trace for a single benchmark:
```
python aerodrome.py <benchmark_name>
```
Here, `<benchmark_name>` could be something like `philo`.

Alternatively, if you have generated `trace.rr` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python aerodrome.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<benchmark_name>/aerodrome.txt`
	- `$AE_HOME/benchmarks/<benchmark_name>/aerodrome.err`
	- `$AE_HOME/benchmarks/<benchmark_name>/aerodrome.tim`

The file `aerodrome.err` should ideally be empty. If it is not empty, it contains error information from the Java command run in the python script `aerodrome.py`.

The file `aerodrome.txt` contains the actual output.
An example is below:
```
2
Analysis complete
Number of events analyzed = 6176
Number of violations found = 1
Time for full analysis = 65 milliseconds
```
The last three lines are the most important lines. 
The last line reports the total time taken.
The penultimate line denotes if a violation is found. If there is no violation, it says `Number of violations found = 0`.
The 3rd last line indicates the total number of events analyzed before the violation was reported (or all events if no violation was reported).
The other lines are for debugging purposes.

The file `aerodrome.tim` reports the time taken.

## Running Velodrome

If you want to analyze the trace for a single benchmark:
```
python velodrome.py <benchmark_name>
```
Here, `<benchmark_name>` could be something like `philo`.

Alternatively, if you have generated `trace.rr` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python velodrome.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<benchmark_name>/velodrome.txt`
	- `$AE_HOME/benchmarks/<benchmark_name>/velodrome.err`
	- `$AE_HOME/benchmarks/<benchmark_name>/velodrome.tim`

The file `velodrome.err` should ideally be empty. If it is not empty, it contains error information from the Java command run in the python script `aerodrome.py`.

The file `velodrome.txt` contains the actual output.
An example is below:
```
2
Analysis complete
Number of events analyzed = 6176
Number of violations found = 1
Number of transactions remaining = 5
Time for full analysis = 61 milliseconds
```
The last four lines are the most important lines. 
The last line reports the total time taken.
The 2nd last line denotes the number of transactions remaining in the transaction graph of Velodrome's analysis at the time the analysis ended.
The 3rd last line denotes if a violation is found. If there is no violation, it says `Number of violations found = 0`.
The 4th last line indicates the total number of events analyzed before the violation was reported (or all events if no violation was reported).
The other lines are for debugging purposes.

The file `velodrome.tim` reports the time taken.


