package engine.racedetectionengine.syncpreserving.distance;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SyncPreservingRaceEngine extends RaceDetectionEngine<SyncPreservingRaceState, SyncPreservingRaceEvent>{

	public SyncPreservingRaceEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SyncPreservingRaceState(this.threadSet);
		handlerEvent = new SyncPreservingRaceEvent();
	}

	@Override
	protected boolean skipEvent(SyncPreservingRaceEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SyncPreservingRaceEvent handlerEvent) {	
		//		if(handlerEvent.getType().isAccessType()){
		//			if(state.verbosity == 1 || state.verbosity == 2){
		//				System.out.println();
		//			}	
		//		}
	}

}
