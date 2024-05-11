In order to support RoadRunner traces, I have added a custom printing tool (that only prints a small subset of events, unlike the pre-existing PrintTool [abbreviated `P`] ).
This tool is called PrintSubsetTool and the supplied file `PrintSubsetTool.java` in this folder is the file that you have to put in the appropriate place.

## Instructions for generating RoadRunner files :

1. Download RoadRunner tool [here](https://github.com/stephenfreund/RoadRunner).
Let us say that RoadRunner sits in ${RoadRunnerRoot} directory.

2. Move the file [`PrintSubsetTool.java`](PrintSubsetTool.java.txt) to the folder ${RoadRunner}/src/rr/simple .
That's it! RoadRunner is so designed as to automatically take care of all the book-keeping when you write a new tool.

3. Compile Roadrunner as you normally would (see `INSTALL.txt`) in ${RoadRunnerRoot} .
Most likely, a simple `ant` (in ${RoadRunnerRoot} ) will do the job.

4. Once RoadRunner is compiled, we can run our tool. 
Let us say we want to print the events of the class `/path/to/class` .
Then, the command you should run is
```
rrrun -noFP -noxml -quiet -noTidGC -tool=PS /path/to/class > /path/to/test.rr
```
`/path/to/test.rr` is the log file that is generated. 
This file is quite readable and you can open it to see the events for yourself.
This is how the file would look like:
```
[main: RoadRunner Agent Loaded.]
[main: Running in FAST Mode]
[RR: Creating Fresh Meta Data]
[main: ----- ----- ----- -----       Meep Meep.      ----- ----- ----- -----]
[main: ]
@  main[tid = 0] started .
@  Enter(0,test/Test.main([Ljava/lang/String;)V) from null
@  Thread-0[tid = 1] started by main[tid = 0].
@   Start(0,1)
.
.
.
```

The option `-noFP` disallows any fast-path optimization that RoadRunner would normally employ. In the absence of this flag, some of the access events are not printed.
The options `-noxml` and `-quiet` reduce the clutter that RoadRunner normally produces.
The option `-noTidGC` is (as of now) an unstable/experimental feature. In the absence of this flag, RoadRunner allows thread-id's of dead threads to be re-used. I did not explicitly handle such traces where you would reuse thread ids. Hence, this flag.	
