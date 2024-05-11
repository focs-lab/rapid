package engine.racedetectionengine.shb.distance;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SHBEngine extends RaceDetectionEngine<SHBState, SHBEvent>{

	public SHBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet);
		handlerEvent = new SHBEvent();
	}

	@Override
	protected boolean skipEvent(SHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SHBEvent handlerEvent) {	
	}
	


}
