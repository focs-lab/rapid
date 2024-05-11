package engine.atomicity.conflictserializability.aerodrome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClockOpt;

public class AerodromeState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	public ArrayList<VectorClockOpt> clockThread; // mathcal{C}
	public ArrayList<VectorClockOpt> clockThreadBegin; // mathcal{C^BEGIN}
	public HashMap<Lock, VectorClockOpt> clockLock; // mathcal{L}
	public HashMap<Variable, VectorClockOpt> clockWriteVariable; // mathcal{W}
	public HashMap<Variable, VectorClockOpt> clockReadVariable; // mathcal{R}
	public HashMap<Variable, VectorClockOpt> clockReadVariableCheck; // mathcal{chR}

	public HashMap<Lock, Thread> lastThreadToRelease; // lastRelThr
	public HashMap<Variable, Thread> lastThreadToWrite; // lastWThr

	private HashMap<Thread, Integer> threadToNestingDepth;

	//Optimization data structures
	public HashMap<Thread, HashSet<Variable>> updateSetThread_write; // UpdateSet^w
	public HashMap<Thread, HashSet<Variable>> updateSetThread_read; // UpdateSet^r
	public HashSet<Variable> staleWrites; // Stale^w
	public HashMap<Variable, HashSet<Thread>> staleReads; // Stale^r

	// Other data-structures to track if parent transaction is alive
	public HashMap<Thread, HashSet<Thread>> threadsForkedInActiveTransaction;
	public HashSet<Thread> parentTransactionIsAlive;

	//parameter flags
	public int verbosity;

	public AerodromeState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClockOpt> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClockOpt(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		this.clockThread = new ArrayList<VectorClockOpt>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);
		for(int tIndex = 0; tIndex < this.numThreads; tIndex ++){
			this.clockThread.get(tIndex).setClockIndex(tIndex, 1);
		}

		this.clockThreadBegin = new ArrayList<VectorClockOpt>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThreadBegin, this.numThreads);

		this.clockLock = new HashMap<Lock, VectorClockOpt> ();

		this.clockWriteVariable = new HashMap<Variable, VectorClockOpt> ();

		this.clockReadVariable = new HashMap<Variable, VectorClockOpt> ();

		this.clockReadVariableCheck = new HashMap<Variable, VectorClockOpt> ();

		this.lastThreadToRelease = new HashMap<Lock, Thread> ();

		this.lastThreadToWrite = new HashMap<Variable, Thread> ();

		this.threadToNestingDepth = new HashMap<Thread, Integer> ();
		for(Thread t: tSet){
			this.threadToNestingDepth.put(t, 0);
		}

		this.updateSetThread_read = new HashMap<Thread, HashSet<Variable>> ();
		this.updateSetThread_write = new HashMap<Thread, HashSet<Variable>> ();
		for(Thread t: tSet){
			this.updateSetThread_read.put(t, new HashSet<Variable> ());
			this.updateSetThread_write.put(t, new HashSet<Variable> ());
		}
		
		this.staleReads = new HashMap<Variable, HashSet<Thread>> ();
		this.staleWrites = new HashSet<Variable> ();
		
		this.parentTransactionIsAlive = new HashSet<Thread> ();
		this.threadsForkedInActiveTransaction = new HashMap<Thread, HashSet<Thread>> ();
		for(Thread t: tSet) {
			this.threadsForkedInActiveTransaction.put(t,  new HashSet<Thread> ());
		}
	}

	// Access methods
	private VectorClockOpt getVectorClockFrom1DArray(ArrayList<VectorClockOpt> arr, int index) {
		return arr.get(index);
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = this.clockThread.get(tIndex).getClockIndex(tIndex);
		this.clockThread.get(tIndex).setClockIndex(tIndex, (Integer)(origVal + 1));
	}

	public VectorClockOpt getVectorClock(ArrayList<VectorClockOpt> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClockOpt getVectorClock(HashMap<Lock, VectorClockOpt> arr, Lock l) {
		return arr.get(l);
	}

	public VectorClockOpt getVectorClock(HashMap<Variable, VectorClockOpt> arr, Variable v) {
		return arr.get(v);
	}

	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			this.clockLock.put(l, new VectorClockOpt(this.numThreads));
		}
		return lockToIndex.get(l);
	}

	public int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.clockReadVariable.put(v, new VectorClockOpt(this.numThreads));
			this.clockReadVariableCheck.put(v, new VectorClockOpt(this.numThreads));
			this.clockWriteVariable.put(v, new VectorClockOpt(this.numThreads));
			this.staleReads.put(v,  new HashSet<Thread> ());
		}
		return variableToIndex.get(v);
	}
	
	public boolean transactionIsActive(Thread t) {
		return threadToNestingDepth.get(t) > 0;
	}
	
	public void incrementNestingDepth(Thread t) {
		int cur_depth = threadToNestingDepth.get(t);
		threadToNestingDepth.put(t,  cur_depth + 1);
	}
	
	public void decrementNestingDepth(Thread t) {
		int cur_depth = threadToNestingDepth.get(t);
		threadToNestingDepth.put(t,  cur_depth - 1);
	}
	
	public boolean checkAndGetClock(VectorClockOpt checkClock, VectorClockOpt fromClock, Thread target) {
		int tIndex = this.threadToIndex.get(target);
		VectorClockOpt C_target_begin = getVectorClock(clockThreadBegin, target);		
		if(C_target_begin.isLessThanOrEqual(checkClock, tIndex) && transactionIsActive(target)) {
			return true;
		}
		VectorClockOpt C_target = getVectorClock(clockThread, target);
		C_target.updateWithMax(C_target, fromClock);
		return false;
	}

	public boolean handshakeAtEndEvent_Optimized(Thread t) {
		boolean violationDetected = false;
		int tIndex = this.threadToIndex.get(t);
		VectorClockOpt C_t_begin = this.clockThreadBegin.get(tIndex);
		VectorClockOpt C_t = this.clockThread.get(tIndex);
		
		for(Thread u: this.threadToIndex.keySet()) {
			if(!u.equals(t)) {
				VectorClockOpt C_u = getVectorClock(clockThread, u);
				if(C_t_begin.isLessThanOrEqual(C_u, tIndex)) {
					violationDetected |= checkAndGetClock(C_t, C_t, u);
					if(violationDetected) break;
				}
			}
		}
		if(violationDetected) return violationDetected;
		
		for(Lock l: this.lockToIndex.keySet()) {
			VectorClockOpt L_l = getVectorClock(clockLock, l);
			if(C_t_begin.isLessThanOrEqual(L_l, tIndex)) {
				L_l.updateWithMax(L_l, C_t);
			}
		}
		
		for(Variable v: this.updateSetThread_write.get(t)) {
			if(staleWrites.contains(v) || lastThreadToWrite.get(v).equals(t)) {
				VectorClockOpt W_v = getVectorClock(clockWriteVariable, v);
				W_v.updateWithMax(W_v, C_t);	
			}
			if(lastThreadToWrite.get(v).equals(t)) {	
				staleWrites.remove(v);
			}
		}
		this.updateSetThread_write.get(t).clear();
		
		for(Variable v: this.updateSetThread_read.get(t)) {
			VectorClockOpt R_v = getVectorClock(clockReadVariable, v);
			R_v.updateWithMax(R_v, C_t);
			VectorClockOpt chR_v = getVectorClock(clockReadVariableCheck, v);			
			updateCheckClock(chR_v, C_t, t);
			staleReads.get(v).remove(t);
		}
		this.updateSetThread_read.get(t).clear();
		
		return violationDetected;
	}

	public void updateSetAtWrite(Thread t, Variable v) {
		int tIndex = this.threadToIndex.get(t);
		VectorClockOpt C_t = this.clockThread.get(tIndex);
		for(Thread u: this.threadToIndex.keySet()) {
			if(transactionIsActive(u)) {
				int uIndex = this.threadToIndex.get(u);
				VectorClockOpt C_u_begin = this.clockThreadBegin.get(uIndex);
				if(C_u_begin.isLessThanOrEqual(C_t, uIndex)) {
					this.updateSetThread_write.get(u).add(v);
				}
			}
		}
	}

	public void updateSetAtRead(Thread t, Variable v) {
		int tIndex = this.threadToIndex.get(t);
		VectorClockOpt C_t = this.clockThread.get(tIndex);
		for(Thread u: this.threadToIndex.keySet()) {
			if(transactionIsActive(u)) {
				int uIndex = this.threadToIndex.get(u);
				VectorClockOpt C_u_begin = this.clockThreadBegin.get(uIndex);
				if(C_u_begin.isLessThanOrEqual(C_t, uIndex)) {
					this.updateSetThread_read.get(u).add(v);
				}
			}
		}
	}

	public void updateCheckClock(VectorClockOpt vc1, VectorClockOpt vc2, Thread t) {
		int tIndex = threadToIndex.get(t);
		vc1.updateMax2WithoutLocal(vc2, tIndex);
	}

	// This assumes that local clocks are not increment for events inside a transaction
	public boolean hasIncomingEdge(Thread t) {
		
		if(parentTransactionIsAlive.contains(t)) return true;
		
		int tIndex = this.threadToIndex.get(t);
		VectorClockOpt C_t_begin = this.clockThreadBegin.get(tIndex);
		VectorClockOpt C_t = this.clockThread.get(tIndex);
		return C_t_begin.isLessThan(C_t);
	}
	
	public void getReadClocksFromStaleThreads(Variable v) {
		VectorClockOpt R_v = getVectorClock(clockReadVariable, v);
		VectorClockOpt chR_v = getVectorClock(clockReadVariableCheck, v);	
		for(Thread u: staleReads.get(v)) {
			VectorClockOpt C_u = getVectorClock(clockThread, u);
			R_v.updateWithMax(R_v, C_u);
			updateCheckClock(chR_v, C_u, u);
		}
		staleReads.get(v).clear();
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