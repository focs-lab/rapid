package engine.racedetectionengine.goldilocks;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class GoldilocksEngine extends RaceDetectionEngine<GoldilocksState, GoldilocksEvent>{
	
	public GoldilocksEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new GoldilocksState(this.threadSet, verbosity);
		handlerEvent = new GoldilocksEvent();
	}
	
	@Override
	protected boolean skipEvent(GoldilocksEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(GoldilocksEvent handlerEvent) {		
	}
	
}
