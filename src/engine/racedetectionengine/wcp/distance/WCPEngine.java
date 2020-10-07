package engine.racedetectionengine.wcp.distance;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.accesstimes.orderedvars.OrderedVarsEngine;
import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class WCPEngine extends RaceDetectionEngine<WCPState, WCPEvent> {

	private HashSet<String> orderedVariables;
	private HashMap<String, HashSet<String>> lockToThreadSet;

	public WCPEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(trace_folder);
		handlerEvent = new WCPEvent();

		boolean time_reporting = true;

		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		RefinedAccessTimesEngine accessTimesEngine = new RefinedAccessTimesEngine(pType,
				trace_folder);
		accessTimesEngine.computeLastAccessTimes();
		OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(pType, trace_folder,
				0);
		orderedVarsEngine.analyzeTrace(true, 0);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");
		}

		this.state = new WCPState(this.threadSet, true);
		this.state.view.lockThreadLastInteraction = accessTimesEngine.lockThreadLast;
		this.state.existsLockReadVariableThreads = accessTimesEngine.existsLockReadVariableThreads;
		this.state.existsLockWriteVariableThreads = accessTimesEngine.existsLockWriteVariableThreads;
		this.state.variableToReadEquivalenceClass = accessTimesEngine.variableToReadEquivalenceClass;
		this.state.variableToWriteEquivalenceClass = accessTimesEngine.variableToWriteEquivalenceClass;
		this.lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
		this.orderedVariables = orderedVarsEngine.getOrdredVars();
	}

	@Override
	protected boolean skipEvent(WCPEvent handlerEvent) {
		if (handlerEvent.getType().isAccessType()) {
			String var_name = handlerEvent.getVariable().getName();
			if (this.orderedVariables.contains(var_name)) {
				return true;
			}
		} else if (handlerEvent.getType().isLockType()) {
			if (lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void postHandleEvent(WCPEvent handlerEvent) {
	}

	@Override
	protected void printCompletionStatus() {
		super.printCompletionStatus();
		if (enablePrintStatus) {
//			System.out.println("Num races = " + state.numRaces);
			System.out.println("Number of 'racy' variables = " + state.racyVars.size());
			System.out.println("Max distance = " + state.maxMinDistance);
			double avg_d = ((double) state.sumMinDistance) / ((double) state.numRaces);
			System.out.println("Avg. distance = " + String.format("%.2f", avg_d));
		}
	}

}
