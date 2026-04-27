package engine.racedetectionengine.seqpreserving;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;

import engine.Engine;
import engine.racedetectionengine.seqpreserving.preprocess.PreprocessEngine;
import event.Thread;
import it.unimi.dsi.fastutil.Hash;
import parse.ParserType;
import parse.rr.ParseRoadRunner;
import parse.std.ParseStandard;

public class PrefixEngine extends Engine<PrefixEvent> {
    protected long eventCount;
    protected long totalSkippedEvents;

    protected int numOfThreads;
    protected int numOfVars;
    protected int numOfLocks;

    protected State state;
    protected String sourceFile;

    HashSet<Long> racyevents = new HashSet<>();
    HashSet<Integer> racyLocs = new HashSet<>();

    HashSet<Integer> protectedVars;
    public boolean partition = false;
    long startTimeAnalysis = 0;

    public PrefixEngine(ParserType pType, String trace_folder, boolean subsumption) {
        super(pType);
        sourceFile = trace_folder;
        eventCount = 0;
        totalSkippedEvents = 0;
        this.initializeReader(trace_folder);
        PreprocessEngine engine = new PreprocessEngine(pType, trace_folder, stdParser);
        engine.analyzeTrace();
        protectedVars = engine.getProtectedVars();
        resetSTDParser();
        handlerEvent = new PrefixEvent();
        eventCount = 0;
        state = new State(numOfThreads, numOfVars, numOfLocks, protectedVars, subsumption);
    }

    protected void analyzeEvent(PrefixEvent handlerEvent, Long eventCount){
		
	}

    public void analyzeTrace() {
		if (this.parserType.isRR()) {
			analyzeTraceRR();
		}
        if (this.parserType.isSTD()) {
			analyzeTraceSTD();
		}
		printCompletionStatus();
    }

    private void analyzeTraceRR() {
        boolean flag = false;
        startTimeAnalysis = System.currentTimeMillis();
        long stopTimeAnalysis = 0;
        while(rrParser.checkAndGetNext(handlerEvent)) {
            eventCount = eventCount + 1;
            analyzeEvent(handlerEvent, eventCount);
            postHandleEvent(handlerEvent);
        }
        if(!flag) {
            stopTimeAnalysis = System.currentTimeMillis();
            System.out.println("Not matched");
        }
        long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
        System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
    }

    private void analyzeTraceSTD() {
        long maxState = 0;
        long maxBirth = 0;
        long startTimeAnalysis = System.currentTimeMillis();
        while(stdParser.hasNext()){
            eventCount = eventCount + 1;
            stdParser.getNextEvent(handlerEvent);
            try{
                if(handlerEvent.Handle(state)) {
                    racyevents.add(eventCount);
                    racyLocs.add(handlerEvent.getLocId());
                }
            }
            catch(OutOfMemoryError oome){
                System.err.println("Number of events = " + Long.toString(eventCount));
                state.printMemory();
                oome.printStackTrace();
            }
            postHandleEvent(handlerEvent);
        }
        long stopTimeAnalysis = System.currentTimeMillis();
        long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Max State Size = " + maxState + " birth = " + maxBirth);
        System.out.println("Racy events = " + racyevents);
        System.out.println("Racy locations = " + racyLocs);
        System.out.println("Number of 'racy' events found = " + racyevents.size());
    }

    protected void initializeReaderRV(String trace_folder) {

    }

	protected void initializeReaderCSV(String trace_file) {

    }

	protected void initializeReaderSTD(String trace_file) {
        stdParser = new ParseStandard(trace_file, true);
        numOfThreads = stdParser.getNumOfThreads();
        numOfVars = stdParser.getNumOfVars();
        numOfLocks = stdParser.getNumOfLocks();
    }

	protected void initializeReaderRR(String trace_file) {
        rrParser = new ParseRoadRunner(trace_file, true);
    }

    protected void resetSTDParser() {
        stdParser.totEvents = 0;
        try{
            stdParser.bufferedReader = new BufferedReader(new FileReader(sourceFile));
        }
        catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + sourceFile + "'");
        }
    }

    protected boolean skipEvent(PrefixEvent handlerEvent) {
        // return !handlerEvent.getType().isAccessType();
        return false;
    }

	protected void postHandleEvent(PrefixEvent handlerEvent) {

    } 
}
