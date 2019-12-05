package engine.atomicity.conflictserializability.aerodrome_basic;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class AerodromeEngine extends AtomicityEngine<AerodromeState, AerodromeEvent>{

	public AerodromeEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new AerodromeState(this.threadSet, verbosity);
		handlerEvent = new AerodromeEvent();
	}

	@Override
	protected boolean skipEvent(AerodromeEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(AerodromeEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
