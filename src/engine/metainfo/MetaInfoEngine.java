package engine.metainfo;

import java.util.HashSet;

import engine.Engine;
import event.Event;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public class MetaInfoEngine extends Engine<Event>{

	private HashSet<Integer> locationIdSet;
	private HashSet<String> threadSet_meta;
	private HashSet<String> variableSet_meta;
	private HashSet<String> lockSet_meta;
	private HashSet<String> readVariableSet;
	private HashSet<String> writeVariableSet;

	int eventCount;
	int readCount;
	int writeCount;
	int acquireCount;
	int releaseCount;
	int forkCount;
	int joinCount;

	public MetaInfoEngine(ParserType pType, String trace_folder) {
		super(pType);

		initializeReader(trace_folder);

		handlerEvent = new Event();

		locationIdSet = new HashSet<Integer> ();
		threadSet_meta = new HashSet<String> ();
		lockSet_meta = new HashSet<String> ();
		variableSet_meta = new HashSet<String> ();
		readVariableSet = new HashSet<String> ();
		writeVariableSet = new HashSet<String> ();

		eventCount = 0;
		readCount = 0;
		writeCount = 0;
		acquireCount = 0;
		releaseCount = 0;
		forkCount = 0;
		joinCount = 0;
	}

	@Override
	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, null);
	}

	@Override
	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
	}

	@Override
	protected void initializeReaderSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file);
	}
	
	@Override
	protected void initializeReaderRR(String trace_file) {
		rrParser = new ParseRoadRunner(trace_file);
	}

	public void analyzeTrace() {
		if(this.parserType.isRV()){
			analyzeTraceRV();
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV();
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD();
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR();
		}
	}

	public void processEvent(){
		locationIdSet.add((Integer)handlerEvent.getLocId());
		threadSet_meta.add(handlerEvent.getThread().getName());


		if(handlerEvent.getType().isRead()){
			readCount = readCount + 1;
			readVariableSet.add(handlerEvent.getVariable().getName());
			variableSet_meta.add(handlerEvent.getVariable().getName());
		}
		if(handlerEvent.getType().isWrite()){
			writeCount = writeCount + 1;
			writeVariableSet.add(handlerEvent.getVariable().getName());
			variableSet_meta.add(handlerEvent.getVariable().getName());
		}
		if(handlerEvent.getType().isAcquire()){
			acquireCount = acquireCount + 1;
			lockSet_meta.add(handlerEvent.getLock().getName());
		}
		if(handlerEvent.getType().isRelease()){
			releaseCount = releaseCount + 1;
			lockSet_meta.add(handlerEvent.getLock().getName());
		}
		if(handlerEvent.getType().isFork()){
			forkCount = forkCount + 1;
		}
		if(handlerEvent.getType().isJoin()){
			joinCount = joinCount + 1;
		}
	}

	public void postAnalysis(){
		System.out.println("Number of locations = " + Integer.toString(locationIdSet.size()));
		System.out.println("Number of threads = " + Integer.toString(threadSet_meta.size()));
		System.out.println("Number of locks = " + Integer.toString(lockSet_meta.size()));
		System.out.println("Number of variables = " + Integer.toString(variableSet_meta.size()));
		System.out.println("Number of variables (read) = " + Integer.toString(readVariableSet.size()));
		System.out.println("Number of variables (write) = " + Integer.toString(writeVariableSet.size()));
		System.out.println();

		System.out.println("Number of events = " + Integer.toString(eventCount));
		System.out.println("Number of read events = " + Integer.toString(readCount));
		System.out.println("Number of write events = " + Integer.toString(writeCount));
		System.out.println("Number of reads+writes = " + Integer.toString(readCount + writeCount));
		System.out.println("Number of acquire events = " + Integer.toString(acquireCount));
		System.out.println("Number of release events = " + Integer.toString(releaseCount));
		System.out.println("Number of fork events = " + Integer.toString(forkCount));
		System.out.println("Number of join events = " + Integer.toString(joinCount));
	}

	public void analyzeTraceCSV() {
		for(eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			processEvent();
		}
		postAnalysis();
	}

	public void analyzeTraceRV() {

		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				eventCount = eventCount + 1;
				processEvent();
			}
		}
		postAnalysis();
	}

	public void analyzeTraceSTD() {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			eventCount = eventCount + 1;
			processEvent();
		}
		postAnalysis();
	}
	
	public void analyzeTraceRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			eventCount = eventCount + 1;
			processEvent();
		}
		postAnalysis();
	}

}
