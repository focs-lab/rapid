package util.trace;

import java.util.HashSet;

import event.Lock;
import event.Thread;
import event.Variable;

public class TraceAndDataSets {

	private Trace trace;

	private HashSet<Thread> threadSet;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;

	public TraceAndDataSets(Trace tr, HashSet<Thread> tSet, HashSet<Lock> lSet, HashSet<Variable> vSet) {
		this.trace = tr;
		this.threadSet = tSet;
		this.lockSet = lSet;
		this.variableSet = vSet;
	}

	public Trace getTrace() {
		return this.trace;
	}

	public HashSet<Thread> getThreadSet() {
		return this.threadSet;
	}

	public HashSet<Lock> getLockSet() {
		return this.lockSet;
	}

	public HashSet<Variable> getVariableSet() {
		return this.variableSet;
	}

}
