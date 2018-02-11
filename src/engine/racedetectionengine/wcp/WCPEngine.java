package engine.racedetectionengine.wcp;

import java.util.HashMap;
import java.util.HashSet;
import engine.accesstimes.AccessTimesEngine;
import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class WCPEngine extends RaceDetectionEngine<WCPState, WCPEvent>{
	
	private HashMap<String, Long> lockEndTimes;
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;

	public WCPEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new WCPState(this.threadSet);
		handlerEvent = new WCPEvent();

		boolean time_reporting = false;

		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}

		AccessTimesEngine accessTimesEngine = new AccessTimesEngine(pType, trace_folder);
		accessTimesEngine.computeLastAccessTimes();

		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");	
		}

		lockEndTimes = accessTimesEngine.lockLast;

		this.state.view.lockThreadLastInteraction = accessTimesEngine.lockThreadLast;
		this.state.existsLockReadVariableThreads = accessTimesEngine.existsLockReadVariableThreads;
		this.state.existsLockWriteVariableThreads = accessTimesEngine.existsLockWriteVariableThreads;
		this.state.variableToReadEquivalenceClass = accessTimesEngine.variableToReadEquivalenceClass;
		this.state.variableToWriteEquivalenceClass = accessTimesEngine.variableToWriteEquivalenceClass;		

		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;		
	}
	
	protected boolean skipEvent(WCPEvent handlerEvent){
		boolean skip = false;
		if(handlerEvent.getType().isAccessType()){
			if(variableToThreadSet.get(handlerEvent.getVariable().getName()).size() <= 1 ){
				skip = true;
			}
		}
		if(handlerEvent.getType().isLockType()){
			if(lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1 ){
				skip = true;
			}
		}
		return skip;
	}
	
	protected void postHandleEvent(WCPEvent handlerEvent){
		if(handlerEvent.getType().isLockType()){
			long currEventIndex = handlerEvent.getAuxId();
			long lockThreadEndIndex = this.state.view.lockThreadLastInteraction.get(handlerEvent.getLock().getName()).get(handlerEvent.getThread().getName());
			if(currEventIndex >= lockThreadEndIndex){
				state.destroyLockThreadStack(handlerEvent.getLock(), handlerEvent.getThread());
			}
			//If the lock has to be deleted, it should be done at the end
			long lockEndIndex = lockEndTimes.get(handlerEvent.getLock().getName());
			if(currEventIndex >= lockEndIndex){
				state.destroyLock(handlerEvent.getLock());
			}
		}
	}
}
