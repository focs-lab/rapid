package engine.racedetectionengine.hb_epoch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class HBEpochState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	private ArrayList<Integer> clockThread;
	public ArrayList<VectorClock> HBPredecessorThread;
	public ArrayList<VectorClock> lastReleaseLock;
	public ArrayList<SemiAdaptiveVC> readVariable;
	public ArrayList<SemiAdaptiveVC> writeVariable;

	public HBEpochState(HashSet<Thread> tSet) {
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}
		
		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		// Initialize clockThread
		this.clockThread = new ArrayList<Integer>();
		for (int i = 0; i < this.numThreads; i++) {
			this.clockThread.add((Integer)1);
		}
		
		// initialize HBPredecessorThread
		this.HBPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.HBPredecessorThread, this.numThreads);

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<VectorClock>();

		// initialize readVariable
		this.readVariable = new ArrayList<SemiAdaptiveVC>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<SemiAdaptiveVC>();
	}
	
	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	
	private int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			
			lastReleaseLock.add(new VectorClock(this.numThreads));
		}
		return lockToIndex.get(l);
	}
	
	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			readVariable	.add(new SemiAdaptiveVC());
			writeVariable	.add(new SemiAdaptiveVC());
		}
		return variableToIndex.get(v);
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.get(tIndex);
	}
	
	public VectorClock generateVectorClockFromClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);

		int tValue = getClockThread(t);
		VectorClock pred = getVectorClock(HBPredecessorThread, t);
		VectorClock hbClock = new VectorClock(pred);
		
		hbClock.setClockIndex(tIndex, tValue);
		return hbClock;
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = clockThread.get(tIndex);
		clockThread.set(tIndex, (Integer)(origVal + 1));
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getVectorClockFrom1DArray(arr, lIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}
	
	private <T> T getTFrom1DArray(ArrayList<T> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	public <T> T getAdaptiveVC(ArrayList<T> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getTFrom1DArray(arr, vIndex);
	}
	
	public int getThreadIndex(Thread t){
		return threadToIndex.get(t);
	}
	
	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = generateVectorClockFromClockThread(thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}