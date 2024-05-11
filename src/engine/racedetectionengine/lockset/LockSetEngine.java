package engine.racedetectionengine.lockset;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class LockSetEngine extends RaceDetectionEngine<LockSetState, LockSetEvent>{
	
	public LockSetEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new LockSetState(this.threadSet, verbosity);
		handlerEvent = new LockSetEvent();
	}
	
	@Override
	protected boolean skipEvent(LockSetEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(LockSetEvent handlerEvent) {		
	}
	
}
