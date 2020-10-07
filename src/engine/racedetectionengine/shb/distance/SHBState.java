package engine.racedetectionengine.shb.distance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class SHBState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> clockThread;
	public ArrayList<VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> readVariable;
	public ArrayList<VectorClock> writeVariable;
	public ArrayList<VectorClock> lastWriteVariable;

	// Book-keeping the last-write's location
	public ArrayList<Integer> lastWriteVariableLocId;

	// Distance stats
	public ArrayList<HashMap<Thread, Long>> readVariableAuxId;
	public ArrayList<HashMap<Thread, Long>> writeVariableAuxId;

	// == stats ==
	public long maxMaxDistance = 0;
	public long sumMaxDistance = 0;
	public long maxMinDistance = 0;
	public long sumMinDistance = 0;
	public long numRaces = 0;
	public HashSet<Integer> racyVars = new HashSet<Integer>();

	// parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;

	public SHBState(HashSet<Thread> tSet) {
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			// System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer) this.numThreads);
			this.numThreads++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr,
			int len) {
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

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<VectorClock>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClock>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClock>();

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClock>();

		// initialize locationIds
		this.lastWriteVariableLocId = new ArrayList<Integer>();

		// distance stats
		this.readVariableAuxId = new ArrayList<HashMap<Thread, Long>>();
		this.writeVariableAuxId = new ArrayList<HashMap<Thread, Long>>();
	}

	// Access methods
	private <E> E getVectorClockFrom1DArray(ArrayList<E> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private int checkAndAddLock(Lock l) {
		if (!lockToIndex.containsKey(l)) {
			// System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;

			lastReleaseLock.add(new VectorClock(this.numThreads));
		}
		return lockToIndex.get(l);
	}

	private int checkAndAddVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			readVariable.add(new VectorClock(this.numThreads));
			writeVariable.add(new VectorClock(this.numThreads));
			lastWriteVariable.add(new VectorClock(this.numThreads));
			lastWriteVariableLocId.add(-1); // Initialize loc id's to be -1
			readVariableAuxId.add(new HashMap<Thread, Long>());
			writeVariableAuxId.add(new HashMap<Thread, Long>());
		}
		return variableToIndex.get(v);
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getVectorClockFrom1DArray(arr, lIndex);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}

	public int getLWLocId(Variable v) {
		int vIndex = checkAndAddVariable(v);
		return this.lastWriteVariableLocId.get(vIndex);
	}

	public void setLWLocId(Variable v, int loc) {
		int vIndex = checkAndAddVariable(v);
		this.lastWriteVariableLocId.set(vIndex, loc);
	}

	public void setIndex(VectorClock vc, Thread t, int val) {
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getIndex(VectorClock vc, Thread t) {
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public void printThreadClock() {
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for (Thread thread : threadToIndex.keySet()) {
			VectorClock C_t = getVectorClock(clockThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public long getMaxAuxId(VectorClock confVC, VectorClock currVC,
			HashMap<Thread, Long> auxId_conf, Thread excludeThread) {
		long maxId = -1;
		for (Thread t : auxId_conf.keySet()) {
			if (t.equals(excludeThread)) {
				continue;
			}
			int tIndex_conf = getIndex(confVC, t);
			int tIndex_curr = getIndex(currVC, t);
			if (tIndex_conf > tIndex_curr) {
				long auxId_VC = auxId_conf.get(t);
				if (maxId < auxId_VC) {
					maxId = auxId_VC;
				}
			}
		}
		return maxId;
	}

	public long getMinAuxId(VectorClock confVC, VectorClock currVC,
			HashMap<Thread, Long> auxId_conf, Thread excludeThread) {
		long minId = -1;
		for (Thread t : auxId_conf.keySet()) {
			if (t.equals(excludeThread)) {
				continue;
			}
			int tIndex_conf = getIndex(confVC, t);
			int tIndex_curr = getIndex(currVC, t);
			if (tIndex_conf > tIndex_curr) {
				long auxId_VC = auxId_conf.get(t);
				if (minId == -1) {
					minId = auxId_VC;
				} else {
					if (minId > auxId_VC) {
						minId = auxId_VC;
					}
				}

			}
		}
		return minId;
	}

	public boolean isThreadRelevant(Thread t) {
		return this.threadToIndex.containsKey(t);
	}

	public void printMemory() {
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err
				.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}