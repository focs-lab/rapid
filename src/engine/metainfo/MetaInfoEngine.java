package engine.metainfo;

import java.util.HashMap;
import java.util.HashSet;

import engine.Engine;
import event.Event;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public class MetaInfoEngine extends Engine<Event> {

	private HashSet<Integer> locationIdSet;
	private HashSet<String> threadSet_meta;
	private HashSet<String> variableSet_meta;
	private HashSet<String> lockSet_meta;
	private HashSet<String> readVariableSet;
	private HashSet<String> writeVariableSet;

	private long eventCount;
	private long readCount;
	private long writeCount;
	private long acquireCount;
	private long releaseCount;
	private long forkCount;
	private long joinCount;
	private long beginCount;
	private long endCount;
	private long branchCount;
	private long outermostBeginCount;
	private HashMap<String, Integer> txn_depth;

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
	}

	public void spitOut() {
		System.out.println("Number of locations = " + Integer.toString(locationIdSet.size()));
		System.out.println("Number of threads = " + Integer.toString(threadSet_meta.size()));
		System.out.println("Number of locks = " + Integer.toString(lockSet_meta.size()));
		System.out.println("Number of variables = " + Integer.toString(variableSet_meta.size()));
		System.out.println("Number of variables (read) = " + Integer.toString(readVariableSet.size()));
		System.out.println("Number of variables (write) = " + Integer.toString(writeVariableSet.size()));
		System.out.println();

		System.out.println("Number of events = " + Long.toString(eventCount));
		System.out.println("Number of read events = " + Long.toString(readCount));
		System.out.println("Number of write events = " + Long.toString(writeCount));
		System.out.println("Number of reads+writes = " + Long.toString(readCount + writeCount));
		System.out.println("Number of acquire events = " + Long.toString(acquireCount));
		System.out.println("Number of release events = " + Long.toString(releaseCount));
		System.out.println("Number of fork events = " + Long.toString(forkCount));
		System.out.println("Number of join events = " + Long.toString(joinCount));
		System.out.println("Number of begin events = " + Long.toString(beginCount));
		System.out.println("Number of end events = " + Long.toString(endCount));
		System.out.println("Number of branch events = " + Long.toString(branchCount));
		System.out.println("Number of outermost begin events = " + Long.toString(outermostBeginCount));
	}
	
	public void analyzeTrace() {
		eventCount = 0;
		readCount = 0;
		writeCount = 0;
		acquireCount = 0;
		releaseCount = 0;
		forkCount = 0;
		joinCount = 0;
		beginCount = 0;
		endCount = 0;
		branchCount = 0;
		
		outermostBeginCount = 0;
		txn_depth = new HashMap<String, Integer> ();

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
		spitOut();
	}

	private void analyzeTraceRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				processEvent();
			}
		}
	}

	private void analyzeTraceCSV() {
		for(int eCount = 0; eCount < trace.getSize(); eCount ++){
			handlerEvent = trace.getEventAt(eCount);
			processEvent();
		}
	}

	private void analyzeTraceSTD() {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			processEvent();
		}
	}

	private void analyzeTraceRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			processEvent();
		}
	}

	private void processEvent(){
		locationIdSet.add((Integer)handlerEvent.getLocId());
		threadSet_meta.add(handlerEvent.getThread().getName());

		eventCount = eventCount + 1;
				
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
		if(handlerEvent.getType().isBegin()){
			beginCount = beginCount + 1;
			
			String t_name = handlerEvent.getThread().getName();
			if(!txn_depth.containsKey(t_name)) {
				txn_depth.put(t_name, 0);
			}
			int curr_depth = txn_depth.get(t_name);
			if(curr_depth == 0) {
				outermostBeginCount = outermostBeginCount + 1;
			}
			txn_depth.put(t_name, curr_depth + 1);
		}
		if(handlerEvent.getType().isEnd()){
			endCount = endCount + 1;
			String t_name = handlerEvent.getThread().getName();
			int curr_depth = txn_depth.get(t_name);
			txn_depth.put(t_name, curr_depth - 1);
		}
	}
	
	private void analyzeTraceRV(Long limit) {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				processEvent();
				if(eventCount > limit) {
					break;
				}
			}
		}
	}

	private void analyzeTraceCSV(Long limit) {
		for(int eCount = 0; eCount < trace.getSize(); eCount ++){
			handlerEvent = trace.getEventAt(eCount);
			processEvent();
			if(eventCount > limit) {
				break;
			}
		}
	}

	private void analyzeTraceSTD(Long limit) {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			processEvent();
			if(eventCount > limit) {
				break;
			}
		}
	}
	
	private void analyzeTraceRR(Long limit) {
		while(rrParser.checkAndGetNext(handlerEvent)){
			processEvent();
			if(eventCount > limit) {
				break;
			}
		}
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
	
	public void analyzeTrace(Long limit) {
		eventCount = 0;
		readCount = 0;
		writeCount = 0;
		acquireCount = 0;
		releaseCount = 0;
		forkCount = 0;
		joinCount = 0;
		beginCount = 0;
		endCount = 0;
		branchCount = 0;
		
		outermostBeginCount = 0;
		txn_depth = new HashMap<String, Integer> ();

		if(this.parserType.isRV()){
			analyzeTraceRV(limit);
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(limit);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(limit);
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR(limit);
		}
		spitOut();
	}

}
