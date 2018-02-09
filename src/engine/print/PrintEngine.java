package engine.print;
import java.util.HashMap;

import engine.Engine;
import event.Event;
import event.EventType;
import event.Thread;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public class PrintEngine extends Engine<Event> {

	int totThreads;
	int currentThreadIdx;
	HashMap<Thread, Integer> tIndexMap;
	static HashMap<EventType, String> type2String;

	private static void initType2String(){
		type2String = new  HashMap<EventType, String> ();
		type2String.put(EventType.ACQUIRE, "acq");
		type2String.put(EventType.RELEASE, "rel");
		type2String.put(EventType.READ, "r");
		type2String.put(EventType.WRITE, "w");
		type2String.put(EventType.FORK, "fork");
		type2String.put(EventType.JOIN, "join");
	}

	public PrintEngine(ParserType pType, String trace_folder) {
		super(pType);
		initializeReader(trace_folder);
		initType2String();
		handlerEvent = new Event();
		tIndexMap = new HashMap<Thread, Integer> ();
		currentThreadIdx = 0;
	}

	public void analyzeTrace(ParserType outputType) {
		if(this.parserType.isRV()){
			analyzeTraceRV(outputType);
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(outputType);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(outputType);
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR(outputType);
		}
	}

	private void analyzeTraceRV(ParserType outputType) {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				if(! skipEvent(handlerEvent)){
					processEvent(outputType);
				}
			}
		}
	}

	private void analyzeTraceSTD(ParserType outputType) {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			if(! skipEvent(handlerEvent)){
				processEvent(outputType);
			}
		}
	}

	private void analyzeTraceCSV(ParserType outputType) {
		for(int eCount = 0; eCount < trace.getSize(); eCount ++){
			handlerEvent = trace.getEventAt(eCount);
			if(! skipEvent(handlerEvent)){
				processEvent(outputType);
			}
		}
	}
	
	private void analyzeTraceRR(ParserType outputType) {
		while(rrParser.checkAndGetNext(handlerEvent)){
			if(! skipEvent(handlerEvent)){
				processEvent(outputType);
			}
		}
	}

	private  boolean skipEvent(Event handlerEvent){
		boolean skip = false;
		return skip;	
	}

	private String toCSV(Event e, int tIdx, int totThreads){
		String csvStr = "";
		String decor_name = "";
		if(e.getType().isAccessType()){
			decor_name = e.getVariable().getName(); 
		}
		else if(e.getType().isLockType()){
			decor_name = e.getLock().getName();
		}
		else if(e.getType().isExtremeType()){
			Integer target_index = this.tIndexMap.get(e.getTarget());
			if(target_index == null){
				decor_name = "-unknown-thread-";
			}
			else{
				decor_name = "T" + target_index;
			}
		}
		for(int i = 0; i <totThreads; i++){
			if(i == tIdx){
				csvStr = csvStr + type2String.get(e.getType()) + "(" + decor_name + ")";
			}
			if(i < totThreads - 1){
				csvStr = csvStr + ",";
			}
		}
		return csvStr;
	}

	private String toStandardFormat(Event e){
		String sensibleStr = e.getThread().getName();
		String decor_name = "";
		if(e.getType().isAccessType()){
			decor_name = e.getVariable().getName(); 
		}
		else if(e.getType().isLockType()){
			decor_name = e.getLock().getName();
		}
		else if(e.getType().isExtremeType()){
			decor_name = e.getTarget().getName();
		}
		sensibleStr = sensibleStr + "|" + type2String.get(e.getType()) + "(" + decor_name + ")" + "|" + e.getLocId();
		return sensibleStr;
	}

	private void processEvent(ParserType outputType){
		Thread t = handlerEvent.getThread();
		if(!this.tIndexMap.containsKey(t)){
			tIndexMap.put(t, currentThreadIdx);
			currentThreadIdx = currentThreadIdx + 1;
		}
		if (outputType.isSTD()){
			System.out.println(toStandardFormat(handlerEvent));
		}
		else if(outputType.isCSV()){
			System.out.println(toCSV(handlerEvent, tIndexMap.get(t), totThreads));
		}
	}

	@Override
	protected void initializeReaderRV(String trace_folder) {
		rvParser = new ParseRVPredict(trace_folder, null);
		this.totThreads = rvParser.getTotalThreads();
	}

	@Override
	protected void initializeReaderCSV(String trace_file) {
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
		this.totThreads = traceAndDataSets.getThreadSet().size();
	}

	@Override
	protected void initializeReaderSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file, true);
		totThreads = stdParser.getThreadSet().size();
	}
	
	@Override
	protected void initializeReaderRR(String trace_file) {
		rrParser = new ParseRoadRunner(trace_file, true);
		totThreads = rrParser.getThreadSet().size();
	}

}
