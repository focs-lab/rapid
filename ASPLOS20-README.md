


## Overview

**Paper** : Atomicity Checking in Linear Time using Vector Clocks, ASPLOS'20
**Artifact Outline** - In the above ASPLOS'20 paper, we present an algorithm *AeroDrome* for checking atomicity violations dynamically. We implemented AeroDrome in our tool [RAPID]([https://github.com/umangm/rapid](https://github.com/umangm/rapid))  and evaluate its performance on benchmark programs. We also compare against previous algorithm [Velodrome](https://dl.acm.org/citation.cfm?id=1375618) which has also been implemented in RAPID.
Here we describe how to run AeroDrome and Velodrome to reproduce results from our paper.

## Requirements

1. Machine with Unix based operating system, such as Ubuntu or MacOS. Our experiments were performed on a  machine with about 30GB RAM. Our experiments work even with less RAM but the performance might differ.
2. Java 1.8 or higher
3. Apache Ant
4. Python 2.7 or higher

## Directory Structure

```
AE/
|--- README.md
|--- LICENSE
|--- atomicity_specs/
|--- benchmarks/
|--- scripts/
```

## Overall Workflow

Our benchmarks are provided in the directory `benchmarks/`.
The overall workflow is pretty simple - 

1. **Trace Generation** - We first need to generate execution logs. We will use [RoadRunner](https://github.com/stephenfreund/RoadRunner) for this.
For each benchmark, we will generate a file called `full_trace.rr` using RoadRunner.
Each log is a sequence of *events*. Each event has the form `e = <t, op>`. Here,  `t` denotes the thread that performed `e` and `op` denotes the operation that was performed in the event `e`. The operation  `op` can be  read or write of a variable `x` (`r(x)` or `w(x)`), acquire or release of a lock `l` (`acq(l)` or `rel(l)`), fork or join of another thread `u` (`fork(u)` or `join(u)`), or `begin`/`end` events denoting the start or completion of a *transaction*. These transaction boundary events arise at method entry and exit points.
We will use our script `scripts/gen_trace.py` to generate one trace log `benchmarks/{b}/full_trace.rr` for every benchmark `b`. These files are in the format we call the `rr` format (short for RoadRunner format). This format is readable in a txt editor.

2. **Accounting for Atomicity Specifications** - We will then account for externally provided atomicity specifications. We have curated atomicity specifications (either collecting them from previous works or creating simpler ones ourselves). 
For every benchmark `b`, we have a file `b.txt`  in the directory`atomicity_specs/` containing a list of methods; these are methods that should be discounted when accounting for transactions. Hence, we remove the transaction `begin` and `end` events corresponding to these methods.
We will use our script `scripts/atom_spec.py` to generate the modified trace log `benchmarks/{b}/trace.std` for every benchmark `b`.  The script `scripts/atom_spec.py` runs the class `ExcludeMethods` in RAPID.
These modified trace log files are in a format we call the `std` (short for Standard) format. [Here]([https://github.com/umangm/rapid#1-std-standard-format](https://github.com/umangm/rapid#1-std-standard-format)) is a description of this format.

**Remark** - Both steps 1. and 2. above use up a lot of RAM and take a lot of time. The files used in the experiments at the time of submitting the paper can be obtained form this link: [https://tinyurl.com/asplos20-aerodrome-traces](https://tinyurl.com/asplos20-aerodrome-traces). 

3. **Analyses** - We then analyze the modified execution logs `trace.std`.
For this, we need our tool [RAPID](https://github.com/umangm/rapid/). RAPID can perform several kinds of analyses on an execution log - 
	- The class [`MetaInfo`]([https://github.com/umangm/rapid/blob/master/src/MetaInfo.java](https://github.com/umangm/rapid/blob/master/src/MetaInfo.java))  in RAPID can be used to determine basic information about the log, including the total number of events, threads, variables, locks etc.
	- The [`Aerodrome`]([https://github.com/umangm/rapid/blob/master/src/Aerodrome.java](https://github.com/umangm/rapid/blob/master/src/Aerodrome.java)) in RAPID determines atomicity violations using our proposed algorithm Aerodrome.
	- The [`Velodrome`]([https://github.com/umangm/rapid/blob/master/src/Velodrome.java](https://github.com/umangm/rapid/blob/master/src/Velodrome.java)) in RAPID determines atomicity violations using the prior state-of-the-art algorithm Velodrome.

**Remark** - In the following we will assume that the directory in which this `README` file is located is stored in the environment variables $AE_HOME.
For example, if your directory is `/path/to/AE/`, then you would execute:
```
export AE_HOME=/path/to/AE/
```
Also, you need to change the variable `home` in the file `scripts/util.py` (line 17) to be the value of $AE_HOME .
Also set the environment variables `JAVA_HOME` and `JVM_ARGS` in the same file.

## Steps 1 & 2: Generating traces and Accounting for Atomicity Specifications

**Warning** - This step will take up a lot of time. Readers interested in simply reproducing the results form the paper can download the traces used in our paper from the following link and move to Step-3 directly.
Link: [https://tinyurl.com/asplos20-aerodrome-traces](https://tinyurl.com/asplos20-aerodrome-traces). 
Once you download traces directly from the above link, make sure to delete the original `benchmarks/` folder completely and replace it with the downloaded folder : 
```
rm -rf $AE_HOME/benchmarks/ #Alternatively you may want to rename this folder to $AE_HOME/backup_benchmarks/
unzip /path/to/downloaded/zip -d $AE_HOME/ #It extracts to a folder called asplos20-ae-traces
mv $AE_HOME/asplos20-ae-traces $AE_HOME/benchmarks/
```

### Download and install Roadrunner : 
```
cd $AE_HOME
git clone git@github.com:stephenfreund/RoadRunner.git
cd $AE_HOME/RoadRunner
wget https://raw.githubusercontent.com/umangm/rapid/master/notes/PrintSubsetTool.java.txt -O $AE_HOME/RoadRunner/src/rr/simple/PrintSubsetTool.java
ant
source msetup
```

### Extract execution logs

If you want to generate full trace for a single benchmark, then you should run :
```
python gen_trace.py <b>
```
Here, `<b>` could be something like `philo`.

Alternatively, you could generate traces for all benchmarks as:
```
python gen_trace.py
```

This step generates files `$AE_HOME/benchmarks/<b>/full_trace.rr`, either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran.


### Account for atomicity specifications
First install RAPID - 
```
cd $AE_HOME
git clone git@github.com:umangm/rapid.git
cd $AE_HOME/rapid
ant jar
```

If you want to modify the trace for a single benchmark:
```
python atom_spec.py <b>
```
Here, `<b>` could be something like `philo`.

Alternatively, you could generate traces for all benchmarks as:
```
python atom_spec.py
```

This step generates files `$AE_HOME/benchmarks/<b>/trace.std`, either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran.

At this point, the files `$AE_HOME/benchmarks/<b>/full_trace.rr` are redundant. You may want to delete them.

##  Step-3: Analyses

### Install RAPID
If not already installed, perform the following steps:
```
cd $AE_HOME
git clone git@github.com:umangm/rapid.git
cd $AE_HOME/rapid
ant jar
```

### Getting Trace metadata

If you want to get the metadata about the trace of a single benchmark:
```
python metainfo.py <b>
```
Here, `<b>` could be something like `philo`.

Alternatively, if you have `trace.std` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python metainfo.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<b>/metainfo.txt`
	- `$AE_HOME/benchmarks/<b>/metainfo.err`
	- `$AE_HOME/benchmarks/<b>/metainfo.tim`

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

The file `metainfo.tim` reports the time taken for this analysis.

### Running Aerodrome

If you want to analyze the trace for a single benchmark:
```
python aerodrome.py <b>
```
Here, `<b>` could be something like `philo`.

Alternatively, if you have generated `trace.std` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python aerodrome.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<b>/aerodrome.txt`
	- `$AE_HOME/benchmarks/<b>/aerodrome.err`
	- `$AE_HOME/benchmarks/<b>/aerodrome.tim`

The file `aerodrome.err` should ideally be empty. If it is not empty, it contains error information from the Java command run in the python script `aerodrome.py`.

The file `aerodrome.txt` contains the actual output.
An example is below:
```
Analysis complete
Number of events analyzed = 6176
Atomicity violation detected.
Time for full analysis = 65 milliseconds
```
The second line indicates that before the analyses finished, the first 6176 events in the trace were analyzed by AeroDrome.
The third line denotes that an atomicity violation was reported by AeroDrome.
In examples when no violation is reported, this line would be `No atomicity violation detected.`
The last line reports the total time taken.

The file `aerodrome.tim` reports the time taken.

### Running Velodrome

If you want to analyze the trace for a single benchmark:
```
python velodrome.py <b>
```
Here, `<b>` could be something like `philo`.

Alternatively, if you have generated `trace.rr` for all benchmarks, you could analyze the traces for all benchmarks as follows.
```
python velodrome.py
```

This step generates the following files either for particular benchmark or for all benchmarks depending upon which of the above two commands you ran:
	- `$AE_HOME/benchmarks/<b>/velodrome.txt`
	- `$AE_HOME/benchmarks/<b>/velodrome.err`
	- `$AE_HOME/benchmarks/<b>/velodrome.tim`

The file `velodrome.err` should ideally be empty. If it is not empty, it contains error information from the Java command run in the python script `aerodrome.py`.

The file `velodrome.txt` contains the actual output.
An example is below:
```
Analysis complete
Number of events analyzed = 6176
Atomicity violation detected.
Number of transactions remaining = 5
Time for full analysis = 61 milliseconds
```
The second line indicates that before the analyses finished, the first 6176 events in the trace were analyzed by Velodrome.
The third line denotes that an atomicity violation was reported by Velodrome.
In examples when no violation is reported, this line would be `No atomicity violation detected.`
The fourth line indicates the total number of events analyzed before the violation was reported (or all events if no violation was reported).
The last line reports the total time taken.

The file `velodrome.tim` reports the time taken.
