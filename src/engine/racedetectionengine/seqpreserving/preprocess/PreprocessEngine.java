package engine.racedetectionengine.seqpreserving.preprocess;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;

import engine.Engine;
import parse.ParserType;
import parse.rr.ParseRoadRunner;
import parse.std.ParseStandard;

public class PreprocessEngine extends Engine<PreprocessEvent> {
    protected long eventCount;
    protected long totalSkippedEvents;

    protected State state;
    protected String sourceFile;

    HashSet<Long> racyevents = new HashSet<>();
    HashSet<Integer> racyLocs = new HashSet<>();

    public boolean partition = false;
    long startTimeAnalysis = 0;

    public PreprocessEngine(ParserType pType, String trace_folder, ParseStandard stdParser) {
        super(pType);
        sourceFile = trace_folder;
        eventCount = 0;
        totalSkippedEvents = 0;
        this.stdParser = stdParser;
        handlerEvent = new PreprocessEvent();
        eventCount = 0;
        state = new State();
    }

    protected void analyzeEvent(PreprocessEvent handlerEvent, Long eventCount){
		
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
        while(rrParser.checkAndGetNext(handlerEvent)) {
            eventCount = eventCount + 1;
            analyzeEvent(handlerEvent, eventCount);
            postHandleEvent(handlerEvent);
        }
    }

    private void analyzeTraceSTD() {
        while(stdParser.hasNext()){
            eventCount = eventCount + 1;
            stdParser.getNextEvent(handlerEvent);
            try{
                handlerEvent.Handle(state);
            }
            catch(OutOfMemoryError oome){
                System.err.println("Number of events = " + Long.toString(eventCount));
                state.printMemory();
                oome.printStackTrace();
            }
            postHandleEvent(handlerEvent);
        }
    }

    protected void initializeReaderRV(String trace_folder) {

    }

	protected void initializeReaderCSV(String trace_file) {

    }

	protected void initializeReaderSTD(String trace_file) {
        stdParser = new ParseStandard(trace_file, true);
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

    public HashSet<Integer> getProtectedVars() {
        return state.getProtectedVars();
    }

    protected boolean skipEvent(PreprocessEvent handlerEvent) {
        // return !handlerEvent.getType().isAccessType();
        return false;
    }

	protected void postHandleEvent(PreprocessEvent handlerEvent) {

    } 
}
