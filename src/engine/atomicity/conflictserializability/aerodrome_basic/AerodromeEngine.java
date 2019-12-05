package engine.atomicity.conflictserializability.thb_basic;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class THBEngine extends AtomicityEngine<THBState, THBEvent>{

	public THBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new THBState(this.threadSet, verbosity);
		handlerEvent = new THBEvent();
	}

	@Override
	protected boolean skipEvent(THBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(THBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
