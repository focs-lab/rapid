package engine.racedetectionengine;

import java.util.HashSet;

import engine.Engine;
import event.Thread;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public abstract class RaceDetectionEngine<St extends State, RDE extends RaceDetectionEvent<St>>
		extends Engine<RDE> {

	public St state;
	protected HashSet<Thread> threadSet;

	protected long eventCount;
	protected Long raceCount;
	protected HashSet<Integer> locIdsOfRacyEvents;
	protected Long totalSkippedEvents;

	protected boolean enablePrintStatus;

	public RaceDetectionEngine(ParserType pType) {
		super(pType);
		enablePrintStatus = true;
	}

	@Override
	protected void initializeReaderRV(String trace_folder) {
		rvParser = new ParseRVPredict(trace_folder, this.threadSet);
	}

	@Override
	protected void initializeReaderCSV(String trace_file) {
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.threadSet = traceAndDataSets.getThreadSet();
		this.trace = traceAndDataSets.getTrace();
	}

	@Override
	protected void initializeReaderSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file, true);
		threadSet = stdParser.getThreadSet();
	}

	@Override
	protected void initializeReaderRR(String trace_file) {
		rrParser = new ParseRoadRunner(trace_file, true);
		threadSet = rrParser.getThreadSet();
	}

	protected abstract boolean skipEvent(RDE handlerEvent);

	protected abstract void postHandleEvent(RDE handlerEvent);

	protected boolean analyzeEvent(RDE handlerEvent, int verbosity, Long eventCount) {
		boolean raceDetected = false;
		try {
			raceDetected = handlerEvent.Handle(state, verbosity);
		} catch (OutOfMemoryError oome) {
			oome.printStackTrace();
			System.err.println("Number of events = " + Long.toString(eventCount));
			state.printMemory();
		}
		if (verbosity == 3 && handlerEvent.getType().isAccessType()) {
			System.out.println("|" + Long.toString(eventCount));
		}

		if (raceDetected) {
			// raceCount ++;

			if (verbosity >= 1) {
				System.out.println("Race detected at event " + eventCount + " : "
						+ handlerEvent.toStandardFormat());
			} else {
				System.out.println(handlerEvent.getLocId());
			}
			this.locIdsOfRacyEvents.add(handlerEvent.getLocId());
		}
		return raceDetected;
	}

	public void analyzeTrace(boolean multipleRace, int verbosity) {
		eventCount = (long) 0;
		raceCount = (long) 0;
		locIdsOfRacyEvents = new HashSet<Integer>();
		totalSkippedEvents = (long) 0;
		if (this.parserType.isRV()) {
			analyzeTraceRV(multipleRace, verbosity);
		} else if (this.parserType.isCSV()) {
			analyzeTraceCSV(multipleRace, verbosity);
		} else if (this.parserType.isSTD()) {
			analyzeTraceSTD(multipleRace, verbosity);
		} else if (this.parserType.isRR()) {
			analyzeTraceRR(multipleRace, verbosity);
		}
		printCompletionStatus();
		postAnalysis();
	}

	protected void postAnalysis() {
	}

	public void analyzeTraceCSV(boolean multipleRace, int verbosity) {
		for (eventCount = 0; eventCount < trace.getSize(); eventCount++) {
			handlerEvent.copyFrom(trace.getEventAt((int) eventCount));
			if (skipEvent(handlerEvent)) {
				totalSkippedEvents = totalSkippedEvents + 1;
			} else {
				boolean raceDetected = analyzeEvent(handlerEvent, verbosity, eventCount);
				if (raceDetected) {
					raceCount++;
					if (!multipleRace) {
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}

	public void analyzeTraceRV(boolean multipleRace, int verbosity) {
		if (rvParser.pathListNotNull()) {
			while (rvParser.hasNext()) {
				eventCount = eventCount + 1;
				rvParser.getNextEvent(handlerEvent);

				if (skipEvent(handlerEvent)) {
					totalSkippedEvents = totalSkippedEvents + 1;
				} else {
					boolean raceDetected = analyzeEvent(handlerEvent, verbosity,
							(long) eventCount);
					if (raceDetected) {
						raceCount++;
						if (!multipleRace) {
							break;
						}
					}
					postHandleEvent(handlerEvent);
				}
			}
		}
	}

	public void analyzeTraceSTD(boolean multipleRace, int verbosity) {
		while (stdParser.hasNext()) {
			eventCount = eventCount + 1;
			stdParser.getNextEvent(handlerEvent);

			if (skipEvent(handlerEvent)) {
				totalSkippedEvents = totalSkippedEvents + 1;
			} else {
				boolean raceDetected = analyzeEvent(handlerEvent, verbosity, eventCount);
				if (raceDetected) {
					raceCount++;
					if (!multipleRace) {
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}

	public void analyzeTraceRR(boolean multipleRace, int verbosity) {
		while (rrParser.checkAndGetNext(handlerEvent)) {
			eventCount = eventCount + 1;
			if (skipEvent(handlerEvent)) {
				totalSkippedEvents = totalSkippedEvents + 1;
			} else {
				boolean raceDetected = analyzeEvent(handlerEvent, verbosity,
						(long) eventCount);
				if (raceDetected) {
					raceCount++;
					if (!multipleRace) {
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}

	@Override
	protected void printCompletionStatus() {
		if (enablePrintStatus) {
			System.out.println("Analysis complete");
			System.out.println(
					"Number of 'racy' events found = " + Long.toString(raceCount));
			System.out.println(
					"Number of 'racy' lines found = " + this.locIdsOfRacyEvents.size());
		}
	}

}
