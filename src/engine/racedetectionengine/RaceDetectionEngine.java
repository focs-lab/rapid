package engine.racedetectionengine;

import java.util.HashSet;

import engine.Engine;
import event.Thread;
import parse.ParserType;

public abstract class RaceDetectionEngine<St extends State, RDE extends RaceDetectionEvent<St>> extends Engine<RDE> {

	public St state;
	protected HashSet<Thread> threadSet;

	public RaceDetectionEngine(ParserType pType) {
		super(pType);
	}

	protected abstract void initializeReaderRV(String trace_folder);

	protected abstract void initializeReaderCSV(String trace_file);

	protected abstract void initializeReaderSTD(String trace_file);

	protected abstract boolean skipEvent(RDE handlerEvent);

	protected abstract void postHandleEvent(RDE handlerEvent);

	protected boolean analyzeEvent(RDE handlerEvent, int verbosity, Long eventCount){
		boolean raceDetected = false;
		try{
			raceDetected = handlerEvent.Handle(state, verbosity);
		}
		catch(OutOfMemoryError oome){
			oome.printStackTrace();
			System.err.println("Number of events = " + Long.toString(eventCount));
			state.printMemory();
		}
		if(verbosity==3 && handlerEvent.getType().isAccessType() ){
			System.out.println("|"+Long.toString(eventCount));
		}

		if (raceDetected) {
			//raceCount ++;
			System.out.println(handlerEvent.getLocId());

		}
		return raceDetected;
	}

	public void analyzeTraceCSV(boolean multipleRace, int verbosity) {
		int eventCount =  0;
		Long raceCount = (long) 0;
		Long totalSkippedEvents = (long) 0;

		for(eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent.copyFrom(trace.getEventAt(eventCount));
			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean raceDetected = analyzeEvent(handlerEvent, verbosity, (long) eventCount);
				if(raceDetected){
					raceCount ++;
					if (!multipleRace){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}		
		System.out.println("Analysis complete");
		System.out.println("Number of 'racy' events found = " + Long.toString(raceCount));
	}

	public void analyzeTraceRV(boolean multipleRace, int verbosity) {
		Long eventCount = (long) 0;
		Long raceCount = (long) 0;
		Long totalSkippedEvents = (long) 0;

		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				eventCount = eventCount + 1;
				rvParser.getNextEvent(handlerEvent);

				if(skipEvent(handlerEvent)){
					totalSkippedEvents = totalSkippedEvents + 1;
				}
				else{
					boolean raceDetected = analyzeEvent(handlerEvent, verbosity, (long) eventCount);
					if(raceDetected){
						raceCount ++;
						if (!multipleRace){
							break;
						}
					}
					postHandleEvent(handlerEvent);
				}
			}
		}
		System.out.println("Analysis complete");
		System.out.println("Number of 'racy' events found = " + Long.toString(raceCount));
	}

	public void analyzeTraceSTD(boolean multipleRace, int verbosity) {
		Long eventCount = (long) 0;
		Long raceCount = (long) 0;
		Long totalSkippedEvents = (long) 0;

		while(stdParser.hasNext()){
			eventCount = eventCount + 1;
			stdParser.getNextEvent(handlerEvent);

			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean raceDetected = analyzeEvent(handlerEvent, verbosity, (long) eventCount);
				if(raceDetected){
					raceCount ++;
					if (!multipleRace){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
		
		System.out.println("Analysis complete");
		System.out.println("Number of 'racy' events found = " + Long.toString(raceCount));
	}


}
