package engine.accesstimes.orderedvars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class OrderedVarsState extends State {

	// ==== Internal data ====
	public HashMap<Integer, Integer> threadToIndex;
	private HashMap<String, Integer> variableToIndex;
	private int numThreads;
	private int numVariables;

	// ==== Data used for algorithm ====

	// Data for maintaining the partial order
	public ArrayList<VectorClock> clockThread;
	public ArrayList<VectorClock> lastWriteVariable;
	public HashMap<String, Integer> thread_lastWriteAccessForVariable; // VarName -> threadId. thread_lastWriteAccessForVariable(x) = thread that performed the last write on x
	public HashSet<String> readVars; // Set of vars x such that the last access on x was a read.
	public ArrayList<VectorClock> lastVariable_read;
	
	public HashSet<String> orderedVariables;
	public HashMap<String, HashSet<String>> lockToThreadSet;
	public HashMap<String, HashSet<String>> variableToThreadSet;
	public HashSet<String> variablesWritten;


		// ==== parameter flags ====
	public boolean tickClockOnAccess;
	public int verbosity;

	public OrderedVarsState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Integer, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			int threadId = tIter.next().getId();
			this.threadToIndex.put(threadId, (Integer) this.numThreads);
			this.numThreads++;
		}

		this.variableToIndex = new HashMap<String, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(
			ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {

		// initialize clockThread
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClock>();
		
		// initialize lastWriteVariable
		this.lastVariable_read = new ArrayList<VectorClock>();
		this.thread_lastWriteAccessForVariable = new HashMap<String, Integer> ();
		this.readVars = new HashSet<String> ();

		// initialize orderedVariables
		this.orderedVariables = new HashSet<String> ();
		
		this.lockToThreadSet = new HashMap<String, HashSet<String>>();
		this.variableToThreadSet = new HashMap<String, HashSet<String>>();
		this.variablesWritten = new HashSet<String> ();
	}

	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr,
			int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	public int checkAndAddVariable(Variable v) {
		if (!variableToIndex.containsKey(v.getName())) {
			variableToIndex.put(v.getName(), this.numVariables);
			this.numVariables++;
			this.lastWriteVariable.add(new VectorClock(this.numThreads));
			this.lastVariable_read.add(new VectorClock(this.numThreads));
			this.orderedVariables.add(v.getName());
		}
		return variableToIndex.get(v.getName());
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t.getId());
		VectorClock C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t.getId());
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}

	public void setClockIndex(VectorClock vc, int tId, int val) {
		int tIndex = threadToIndex.get(tId);
		vc.setClockIndex(tIndex, val);
	}

	public int getClockIndex(VectorClock vc, int tId) {
		int tIndex = threadToIndex.get(tId);
		return vc.getClockIndex(tIndex);
	}
	
	public boolean isThreadRelevant(Thread t) {
		return this.threadToIndex.containsKey(t.getId());
	}

	public void printMemory() {
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err
		.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}