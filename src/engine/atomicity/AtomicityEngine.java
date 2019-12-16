package engine.atomicity;

import java.util.HashSet;

import engine.Engine;
import event.Thread;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public abstract class AtomicityEngine<St extends State, RDE extends AtomicityEvent<St>> extends Engine<RDE> {

	public St state;
	protected HashSet<Thread> threadSet;

	protected long eventCount;
	protected Long violationCount;
	protected Long totalSkippedEvents;

	public AtomicityEngine(ParserType pType) {
		super(pType);
	}

	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, this.threadSet);
	}

	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.threadSet = traceAndDataSets.getThreadSet();
		this.trace = traceAndDataSets.getTrace();
	}
	
	protected void initializeReaderSTD(String trace_folder){
		stdParser = new ParseStandard(trace_folder, true);
		threadSet = stdParser.getThreadSet();
	}
	
	protected void initializeReaderRR(String trace_folder){
		rrParser = new ParseRoadRunner(trace_folder,  true);
		threadSet = rrParser.getThreadSet();
	}
	
	protected boolean analyzeEvent(RDE handlerEvent, Long eventCount){
		boolean violationDetected = false;
		try{
			violationDetected = handlerEvent.Handle(state);
		}
		catch(OutOfMemoryError oome){
			oome.printStackTrace();
			System.err.println("Number of events = " + Long.toString(eventCount));
			state.printMemory();
		}
		if(state.verbosity==3 && handlerEvent.getType().isAccessType() ){
			System.out.println("|"+Long.toString(eventCount));
		}

//		if (violationDetected) {
//			System.out.println(handlerEvent.getLocId());
//		}
		return violationDetected;
	}

	public void analyzeTrace(boolean multipleRace){
		eventCount =  (long) 0;
		violationCount = (long) 0;
		totalSkippedEvents = (long) 0;
		if(this.parserType.isRV()){
			analyzeTraceRV(multipleRace);		
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(multipleRace);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(multipleRace);
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR(multipleRace);
		}

		System.out.println("Analysis complete");
		if(violationCount > 0) {
			System.out.println("Atomicity violation detected");
		}
		else {
			System.out.println("No atomicity violation detected");
		}
		System.out.println("Number of events analyzed = " + Long.toString(eventCount));
	}

	protected void analyzeTraceCSV(boolean multipleRace) {
		for(eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent.copyFrom(trace.getEventAt((int)eventCount));
			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean raceDetected = analyzeEvent(handlerEvent, eventCount);
				if(raceDetected){
					violationCount ++;
					if (!multipleRace){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}		
	}

	protected void analyzeTraceRV(boolean multipleRace) {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				eventCount = eventCount + 1;
				rvParser.getNextEvent(handlerEvent);

				if(skipEvent(handlerEvent)){
					totalSkippedEvents = totalSkippedEvents + 1;
				}
				else{
					boolean raceDetected = analyzeEvent(handlerEvent, (long) eventCount);
					if(raceDetected){
						violationCount ++;
						if (!multipleRace){
							break;
						}
					}
					postHandleEvent(handlerEvent);
				}
			}
		}
	}

	protected void analyzeTraceSTD(boolean multipleRace) {
		while(stdParser.hasNext()){
			eventCount = eventCount + 1;
			stdParser.getNextEvent(handlerEvent);

			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean violationDetected = analyzeEvent(handlerEvent, (long) eventCount);
				if(violationDetected){
					violationCount ++;
					if (!multipleRace){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}
	
	protected void analyzeTraceRR(boolean multipleRace) {
		while(rrParser.checkAndGetNext(handlerEvent)){
			eventCount = eventCount + 1;

			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean violationDetected = analyzeEvent(handlerEvent, (long) eventCount);
				if(violationDetected){
					violationCount ++;
					if (!multipleRace){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}

	protected abstract boolean skipEvent(RDE handlerEvent);

	protected abstract void postHandleEvent(RDE handlerEvent);

}
