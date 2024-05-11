package engine.racedetectionengine.hb;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class HBEngine extends RaceDetectionEngine<HBState, HBEvent> {

	public HBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new HBState(this.threadSet);
		this.handlerEvent = new HBEvent();
	}
	
	protected boolean skipEvent(HBEvent handlerEvent){
		return false;
	}
	
	protected void postHandleEvent(HBEvent handlerEvent){}
}
