package engine.racedetection.syncreversalfree.distance;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SRFreeEngine extends RaceDetectionEngine<SRFreeState, SRFreeEvent>{

	public SRFreeEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SRFreeState(this.threadSet);
		handlerEvent = new SRFreeEvent();
	}

	@Override
	protected boolean skipEvent(SRFreeEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SRFreeEvent handlerEvent) {	
		//		if(handlerEvent.getType().isAccessType()){
		//			if(state.verbosity == 1 || state.verbosity == 2){
		//				System.out.println();
		//			}	
		//		}
	}

}
