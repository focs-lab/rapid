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
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}
	
	@Override
	protected void printCompletionStatus() {
		super.printCompletionStatus();
		if(enablePrintStatus) {
			System.out.println("Num races = " + state.numRaces);
//			System.out.println("Max MaxRace distance = " + state.maxMaxDistance);
			System.out.println("Max distance = " + state.maxMaxDistance);
			double avg_d_max = ((double) state.sumMaxDistance) / ((double) state.numRaces);
			System.out.println("Avg. MaxRace distance = " + avg_d_max);
			System.out.println("Avg. distance = " + avg_d_max);
			
//			System.out.println("Max MinRace distance = " + state.maxMinDistance);
//			double avg_d_min = ((double) state.sumMinDistance) / ((double) state.numRaces);
//			System.out.println("Avg. MinRace distance = " + avg_d_min);
		}
	}


}
