package engine.racedetectionengine.fhb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

class IntegerPair {

    private final int fst;
    private final int snd;

    public IntegerPair(int x, int y) {
    	if(x <= y){
    		this.fst = x;
    		this.snd = y;
    	}
    	else{
    		this.fst = y;
    		this.snd = x;
    	}
    }

    public int getFst() {
        return this.fst;
    }

    public int getSnd() {
        return this.snd;
    }
    
    @Override 
    public String toString(){
    	return "<" + Integer.toString(this.fst) + "|" + Integer.toString(this.snd) + ">";
    }

    @Override 
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof IntegerPair) {
            IntegerPair that = (IntegerPair) other;
            result = (this.getFst() == that.getFst() && this.getSnd() == that.getSnd());
        }
        return result;
    }

    @Override 
    public int hashCode() {
        return (41 * (41 + getFst()) + getSnd());
    }
}

public class FHBState extends State {

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
	
	public int eventId;
	
	//Book-keeping the last-write's location
	public ArrayList<Integer> lastWriteVariableLocId; //variable -> loc
	//Book-keeping the location of the last-read by every thread
	public ArrayList<ArrayList<Integer>> lastReadVariableLocId; //variable -> thread -> loc 
	public ArrayList<ArrayList<VectorClock>> lastReadVariableClock;
	public ArrayList<ArrayList<Integer>> lastReadVariableEventId;
	
	//Tracking the pc-pairs in race
	private HashSet<IntegerPair> raceLocPairs;
	
	public FHBState(HashSet<Thread> tSet) {
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
		
		this.eventId = 0;
		
		//initialize locationIds
		this.lastWriteVariableLocId = new ArrayList<Integer> ();
		this.lastReadVariableLocId = new ArrayList<ArrayList<Integer>> ();
		this.lastReadVariableClock = new ArrayList<ArrayList<VectorClock>> ();
		this.lastReadVariableEventId = new ArrayList<ArrayList<Integer>> ();
		
		this.raceLocPairs = new HashSet<IntegerPair> ();
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
			this.readVariable			.add(new VectorClock(this.numThreads));
			this.writeVariable			.add(new VectorClock(this.numThreads));
			this.lastWriteVariableLocId	.add(-1); //Initialize loc id's to be -1
			this.lastReadVariableLocId	.add(new ArrayList<Integer> ());
			this.lastReadVariableClock	.add(new ArrayList<VectorClock> ());
			this.lastReadVariableEventId.add(new ArrayList<Integer> ());
			for (int i = 0; i < this.numThreads; i++) {
				lastReadVariableLocId.get(this.numVariables).add(-1);
				lastReadVariableClock.get(this.numVariables).add(new VectorClock(this.numThreads));
				lastReadVariableEventId.get(this.numVariables).add(-1);
			}
			this.numVariables ++;
		}
		return variableToIndex.get(v);
	}
	
	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
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

	public int getLWLocId(Variable v){
		int vIndex = checkAndAddVariable(v);
		return this.lastWriteVariableLocId.get(vIndex);
	}
	
	public void setLWLocId(Variable v, int loc){
		int vIndex = checkAndAddVariable(v);
		this.lastWriteVariableLocId.set(vIndex, loc);
	}
	
	public int getLRLocId(Variable v, Thread t){
		int vIndex = checkAndAddVariable(v);
		int tIndex = threadToIndex.get(t);
		return this.lastReadVariableLocId.get(vIndex).get(tIndex);
	}
	
	public int getThread(Thread t){
		return this.threadToIndex.get(t);
	}
	
	public void setLastReadData(Variable v, Thread t, int loc, VectorClock C_t){
		int vIndex = checkAndAddVariable(v);
		int tIndex = threadToIndex.get(t);
		this.lastReadVariableLocId.get(vIndex).set(tIndex, loc);
		this.eventId = this.eventId + 1;
		this.lastReadVariableEventId.get(vIndex).set(tIndex, this.eventId);
		this.lastReadVariableClock.get(vIndex).get(tIndex).copyFrom(C_t);
	}

	public boolean checkRaceWithReadsAndAddLocPairs(Thread t, Variable v, VectorClock C_t, int locId){
		boolean raceDetected = false;
		int vIndex = checkAndAddVariable(v);
		int maxEventId = -1;
		int rLocId = -1;
		for(int uIndex = 0; uIndex < this.numThreads; uIndex ++){
			VectorClock R_v = this.lastReadVariableClock.get(vIndex).get(uIndex);
			if(! R_v.isLessThanOrEqual(C_t)){
				raceDetected = true;
				int eventId = this.lastReadVariableEventId.get(vIndex).get(uIndex);
				if(eventId > maxEventId){
					maxEventId = eventId;
					rLocId = this.lastReadVariableLocId.get(vIndex).get(uIndex);
				}	
			}
		}
		if (raceDetected){
			this.addLocPair(rLocId, locId);
		}	
		return raceDetected;
	}
		
	public void addLocPair(int i1, int i2){
		if (i1 < 0 || i2 < 0){
			throw new IllegalArgumentException("WTF");
		}
		IntegerPair intpr = new IntegerPair(i1, i2);
		this.raceLocPairs.add(intpr);
	}
	
	public HashSet<IntegerPair> getLocPairs(){
		return this.raceLocPairs;
	}
	
	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}
	
	public int getThreadIndex(Thread t){
		return threadToIndex.get(t);
	}
	
	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = getVectorClock(clockThread, thread);
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