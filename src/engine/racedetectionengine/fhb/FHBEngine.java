package engine.racedetectionengine.fhb;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class FHBEngine extends RaceDetectionEngine<FHBState, FHBEvent>{

	public FHBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new FHBState(this.threadSet);
		handlerEvent = new FHBEvent();
	}

	public void postAnalysis(){
//		System.out.println("Number of race pc pairs = " + Integer.toString(state.getLocPairs().size()));
	}
	
	@Override
	protected boolean skipEvent(FHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(FHBEvent handlerEvent) {	
	}

}
