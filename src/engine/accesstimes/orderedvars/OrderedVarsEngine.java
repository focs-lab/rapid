package engine.accesstimes.orderedvars;

import java.util.HashMap;
import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class OrderedVarsEngine extends RaceDetectionEngine<OrderedVarsState, OrderedVarsEvent>{

	public OrderedVarsEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new OrderedVarsState(this.threadSet, verbosity);
		handlerEvent = new OrderedVarsEvent();
		this.enablePrintStatus = false;
	}

	@Override
	protected boolean skipEvent(OrderedVarsEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(OrderedVarsEvent handlerEvent) {
	}
	
	public HashSet<String> getOrdredVars(){
		return this.state.orderedVariables;
	}
	
	public HashMap<String, HashSet<String>> getVariableToThreadSet(){
		return this.state.variableToThreadSet;
	}
	
	public HashMap<String, HashSet<String>> getLockToThreadSet(){
		return this.state.lockToThreadSet;
	}	
}
