package engine.racedetectionengine.wcp.distance;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import engine.Engine;
import event.Event;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rv.ParseRVPredict;
import util.trace.TraceAndDataSets;

public class WCPAnnotationEngine extends Engine<Event> {

	private HashSet<Integer> locationIdSet;

	public HashMap<String, Long> threadFirst;
	public HashMap<String, Long> threadLast;
	public HashMap<String, Long> threadInterval;

	public HashMap<String, Long> lockFirst;
	public HashMap<String, Long> lockLast;
	public HashMap<String, Long> lockInterval;

	public HashMap<String, Long> variableFirst;
	public HashMap<String, Long> variableLast;
	public HashMap<String, Long> variableInterval;

	public HashMap<String, Long> readVariableFirst;
	public HashMap<String, Long> readVariableLast;
	public HashMap<String, Long> readVariableInterval;

	public HashMap<String, Long> writeVariableFirst;
	public HashMap<String, Long> writeVariableLast;
	public HashMap<String, Long> writeVariableInterval;
	
	private int eventCount;
	private int readCount;
	private int writeCount;
	private int acquireCount;
	private int releaseCount;
	private int forkCount;
	private int joinCount;

	public WCPAnnotationEngine(ParserType pType, String trace_folder) {

		super(pType);
		initializeReader(trace_folder);

		handlerEvent = new Event();

		locationIdSet = new HashSet<Integer> ();

		threadFirst             = new HashMap<String, Long> () ;
		threadLast              = new HashMap<String, Long> () ;
		threadInterval          = new HashMap<String, Long> () ;

		lockFirst               = new HashMap<String, Long> () ;
		lockLast                = new HashMap<String, Long> () ;
		lockInterval            = new HashMap<String, Long> () ;

		variableFirst           = new HashMap<String, Long> () ;
		variableLast            = new HashMap<String, Long> () ;
		variableInterval        = new HashMap<String, Long> () ;

		readVariableFirst       = new HashMap<String, Long> () ;
		readVariableLast        = new HashMap<String, Long> () ;
		readVariableInterval    = new HashMap<String, Long> () ;

		writeVariableFirst      = new HashMap<String, Long> () ;
		writeVariableLast       = new HashMap<String, Long> () ;
		writeVariableInterval   = new HashMap<String, Long> () ;
	}

	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, null);
	}

	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
	}

	private void processEvent(){
		locationIdSet.add((Integer)handlerEvent.getLocId());
		long eventIndex = handlerEvent.getAuxId();
		if(! threadFirst.containsKey(handlerEvent.getThread().getName())){
			threadFirst.put(handlerEvent.getThread().getName(), eventIndex);
		}
		threadLast.put(handlerEvent.getThread().getName(), eventIndex);

		if(handlerEvent.getType().isRead()){
			readCount = readCount + 1;
			if(! readVariableFirst.containsKey(handlerEvent.getVariable().getName())){
				readVariableFirst.put(handlerEvent.getVariable().getName(), eventIndex);
			}
			readVariableLast.put(handlerEvent.getVariable().getName(), eventIndex);
			if(! variableFirst.containsKey(handlerEvent.getVariable().getName())){
				variableFirst.put(handlerEvent.getVariable().getName(), eventIndex);
			}
			variableLast.put(handlerEvent.getVariable().getName(), eventIndex);
		}

		if(handlerEvent.getType().isWrite()){
			writeCount = writeCount + 1;
			if(! writeVariableFirst.containsKey(handlerEvent.getVariable().getName())){
				writeVariableFirst.put(handlerEvent.getVariable().getName(), eventIndex);
			}
			writeVariableLast.put(handlerEvent.getVariable().getName(), eventIndex);
			if(! variableFirst.containsKey(handlerEvent.getVariable().getName())){
				variableFirst.put(handlerEvent.getVariable().getName(), eventIndex);
			}
			variableLast.put(handlerEvent.getVariable().getName(), eventIndex);
		}

		if(handlerEvent.getType().isAcquire()){	
			acquireCount = acquireCount + 1;
			if(! lockFirst.containsKey(handlerEvent.getLock().getName())){
				lockFirst.put(handlerEvent.getLock().getName(), eventIndex);
			}
			lockLast.put(handlerEvent.getLock().getName(), eventIndex);
		}

		if(handlerEvent.getType().isRelease()){
			releaseCount = releaseCount + 1;
			if(! lockFirst.containsKey(handlerEvent.getLock().getName())){
				lockFirst.put(handlerEvent.getLock().getName(), eventIndex);
			}
			lockLast.put(handlerEvent.getLock().getName(), eventIndex);
		}
		
		if(handlerEvent.getType().isFork()){
			forkCount = forkCount + 1;
		}
		
		if(handlerEvent.getType().isJoin()){
			joinCount = joinCount + 1;
		}
	}

	private void postAnalyzeTrace(){
		System.out.println("Number of locations = " + Integer.toString(locationIdSet.size()));

		System.out.println("Number of threads = " + Integer.toString(threadFirst.size()));
		System.out.println("Number of locks = " + Integer.toString(lockFirst.size()));
		System.out.println("Number of variables = " + Integer.toString(variableFirst.size()));
		System.out.println("Number of variables (read) = " + Integer.toString(readVariableFirst.size()));
		System.out.println("Number of variables (write) = " + Integer.toString(writeVariableFirst.size()));
		System.out.println();

		System.out.println("Number of events = " + Integer.toString(eventCount));
		System.out.println("Number of read events = " + Integer.toString(readCount));
		System.out.println("Number of write events = " + Integer.toString(writeCount));
		System.out.println("Number of reads+writes = " + Integer.toString(readCount + writeCount));
		System.out.println("Number of acquire events = " + Integer.toString(acquireCount));
		System.out.println("Number of release events = " + Integer.toString(releaseCount));
		System.out.println("Number of fork events = " + Integer.toString(forkCount));
		System.out.println("Number of join events = " + Integer.toString(joinCount));

		setDifferenceMap(threadFirst,			threadLast,			threadInterval);
		setDifferenceMap(lockFirst,				lockLast,			lockInterval);
		setDifferenceMap(variableFirst,			variableLast,		variableInterval);
		setDifferenceMap(readVariableFirst,		readVariableLast,	readVariableInterval);
		setDifferenceMap(writeVariableFirst,	writeVariableLast,	writeVariableInterval);

		Long maxThreadLife				= Collections.max(threadInterval.values());
		Long minThreadLife				= Collections.min(threadInterval.values());
		double meanThreadLife			= getMean(threadInterval);

		Long maxLockLife				= Collections.max(lockInterval.values());
		Long minLockLife				= Collections.min(lockInterval.values());
		double meanLockLife				= getMean(lockInterval);

		Long maxVariableLife			= Collections.max(variableInterval.values());
		Long minVariableLife			= Collections.min(variableInterval.values());
		double meanVariableLife			= getMean(variableInterval);

		Long maxReadVariableLife		= Collections.max(readVariableInterval.values());
		Long minReadVariableLife		= Collections.min(readVariableInterval.values());
		double meanReadVariableLife		= getMean(readVariableInterval);

		Long maxWriteVariableLife		= Collections.max(writeVariableInterval.values());
		Long minWriteVariableLife		= Collections.min(writeVariableInterval.values());
		double meanWriteVariableLife	= getMean(writeVariableInterval);

		System.out.println();

		System.out.println("Life of threads : ");
		System.out.println("\t Max = " + Long.toString(maxThreadLife));
		System.out.println("\t Min = " + Long.toString(minThreadLife));
		System.out.println("\t Mean = " + Double.toString(meanThreadLife));

		System.out.println("Life of locks : ");
		System.out.println("\t Max = " + Long.toString(maxLockLife));
		System.out.println("\t Min = " + Long.toString(minLockLife));
		System.out.println("\t Mean = " + Double.toString(meanLockLife));

		System.out.println("Life of variables : ");
		System.out.println("\t Max = " + Long.toString(maxVariableLife));
		System.out.println("\t Min = " + Long.toString(minVariableLife));
		System.out.println("\t Mean = " + Double.toString(meanVariableLife));

		System.out.println("Life of read variables : ");
		System.out.println("\t Max = " + Long.toString(maxReadVariableLife));
		System.out.println("\t Min = " + Long.toString(minReadVariableLife));
		System.out.println("\t Mean = " + Double.toString(meanReadVariableLife));

		System.out.println("Life of write variables : ");
		System.out.println("\t Max = " + Long.toString(maxWriteVariableLife));
		System.out.println("\t Min = " + Long.toString(minWriteVariableLife));
		System.out.println("\t Mean = " + Double.toString(meanWriteVariableLife));
	}


	public void analyzeTrace(){
		
		eventCount = 0;
		readCount = 0;
		writeCount = 0;
		acquireCount = 0;
		releaseCount = 0;
		forkCount = 0;
		joinCount = 0;
		
		if(this.parserType.isRV()){
			analyzeTraceRV();
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV();
		}
		postAnalyzeTrace();
	}

	private void analyzeTraceCSV() {
		for(eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			processEvent();
		}
	}


	private void analyzeTraceRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				eventCount = eventCount + 1;
				processEvent();
			}
		}
	}
	
	private void getLockLastAccessTimesRV(){
		long eventIndex = 0;
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				eventIndex = handlerEvent.getAuxId();
				if(handlerEvent.getType().isLockType()){
					lockLast.put(handlerEvent.getLock().getName(), eventIndex);
				}
			}
		}
	}
	
	private void getLockLastAccessTimesCSV(){
		long eventIndex = 0;
		for(int eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			eventIndex = handlerEvent.getAuxId();
			if(handlerEvent.getType().isLockType()){
				lockLast.put(handlerEvent.getLock().getName(), eventIndex);
			}
		}
	}

	public HashMap<String, Long> getLockLastAccessTimes() {
		if(this.parserType.isRV()){
			getLockLastAccessTimesRV();
		}
		else if(this.parserType.isCSV()){
			getLockLastAccessTimesCSV();
		}
		return lockLast;
	}

	private void setDifferenceMap(HashMap<String, Long> first, HashMap<String, Long> last, HashMap<String, Long> interval){
		for(String str : first.keySet()){
			if (! last.containsKey(str)){
				throw new IllegalArgumentException(str + " key not found");
			}
			interval.put(str, last.get(str) - first.get(str) + 1);
		}
	}

	private double getMean(HashMap<String, Long> mp){
		double sum = 0.0;
		double cnt = 0.0;
		for(Long num : mp.values()){
			sum = sum + num;
			cnt = cnt + 1.0;
		}
		return sum/cnt;
	}

	@Override
	protected void initializeReaderSTD(String trace_file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void initializeReaderRR(String trace_file) {
		// TODO Auto-generated method stub
		
	}
}
