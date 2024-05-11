package engine.racedetectionengine.wcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

//Manages the clocks and other data structures used by the WCP algorithm
public class WCPState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;
	
	private int numThreads;

	// Data used for algorithm
	private ArrayList<Integer> clockThread;
	private ArrayList<VectorClock> WCPThread;
	public ArrayList<VectorClock> HBPredecessorThread;
	public HashMap<Lock, VectorClock> HBPredecessorLock;
	//public HashMap<Lock, VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> WCPPredecessorThread;
	public HashMap<Lock, VectorClock> WCPPredecessorLock;
	
	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>> lastReleaseLockReadVariableThread;
	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>> lastReleaseLockWriteVariableThread;
	
	public HashMap<Variable, VectorClock> readVariable;
	public HashMap<Variable, VectorClock> writeVariable;
	/***The next two clocks are relevant for and only for the "forceOrder" feature ***/
	public HashMap<Variable, VectorClock> HBPredecessorReadVariable;
	public HashMap<Variable, VectorClock> HBPredecessorWriteVariable;
	/*********************************************************************************/
	public WCPView view;
	
	//Data used for online tracking of locks and variables
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<Lock>> mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	
	//Data for offline space saving
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockReadVariableThreads;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockWriteVariableThreads;
	
	public HashMap<String, Long> variableToReadEquivalenceClass;
	public HashMap<String, Long> variableToWriteEquivalenceClass;
	
	//Space saving
	private VectorClock local_vc_imax;

	public WCPState(HashSet<Thread> tSet) {
		initInternalData(tSet);
		initData(tSet);
		initOnlineData();	
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
				
		this.lockSet = new HashSet<Lock>();
		this.variableSet = new HashSet<Variable>();
		
		local_vc_imax = new VectorClock(this.numThreads);
	}

	public void initData(HashSet<Thread> tSet) {
		// Initialize clockThread
		this.clockThread = new ArrayList<Integer>();
		for (int i = 0; i < this.numThreads; i++) {
			this.clockThread.add((Integer)1);
		}
		
		// initialize WCPThread
		this.WCPThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPThread, this.numThreads, this.numThreads);

		// initialize HBPredecessorThread
		this.HBPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.HBPredecessorThread, this.numThreads, this.numThreads);

		// initialize HBPredecessorLock
		this.HBPredecessorLock = new HashMap<Lock, VectorClock>();

		// initialize WCPPredecessorThread
		this.WCPPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPPredecessorThread, this.numThreads, this.numThreads);

		// initialize WCPPredecessorLock
		this.WCPPredecessorLock = new HashMap<Lock, VectorClock>();

		// initialize lastReleaseLockReadVariable
		this.lastReleaseLockReadVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>>();

		// initialize readVariable
		this.readVariable = new HashMap<Variable, VectorClock>();

		// initialize writeVariable
		this.writeVariable = new HashMap<Variable, VectorClock>();
		
		// initialize HBPredecessorReadVariable
		this.HBPredecessorReadVariable = new HashMap<Variable, VectorClock>();

		// initialize HBPredecessorWriteVariable
		this.HBPredecessorWriteVariable = new HashMap<Variable, VectorClock>();

		// initialize view
		this.view = new WCPView(tSet);
	}
	
	public void initOnlineData(){
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	//Access Methods
	void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len, int dim) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(dim));
		}
	}
	
	//Keep track of locks: If a new lock is seen, add it to lockSet.
	private void checkAndAddLock(Lock l){
		if(!lockSet.contains(l)){
			lockSet.add(l);
			
			HBPredecessorLock.put(l, new VectorClock(this.numThreads));
			WCPPredecessorLock.put(l, new VectorClock(this.numThreads));
			
			String lName = l.getName();
			if(existsLockReadVariableThreads.containsKey(lName)){
				this.lastReleaseLockReadVariableThread.put(l, new HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>> ());
				for (Variable v : variableSet){
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if(existsLockReadVariableThreads.get(lName).containsKey(vName)){
						if(!lastReleaseLockReadVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockReadVariableThread.get(l).put(
									vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());
						}
						if(!lastReleaseLockReadVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).put(
									vEquivWrite, new HashMap<Thread, VectorClock>());
						}
						for(Thread t : this.threadToIndex.keySet()){
							if(existsLockReadVariableThreads.get(lName).get(vName).contains(t.getName())){
								this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
							}
						}
					}

				}
			}

			if(existsLockWriteVariableThreads.containsKey(lName)){
				this.lastReleaseLockWriteVariableThread.put(l, new HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>> ());
				for (Variable v : variableSet){
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if(existsLockWriteVariableThreads.get(lName).containsKey(vName)){
						if(!lastReleaseLockWriteVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, VectorClock>>());
						}
						if(!lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).put(vEquivWrite,
									new HashMap<Thread, VectorClock>());
						}
						for(Thread t : this.threadToIndex.keySet()){
							if(existsLockWriteVariableThreads.get(lName).get(vName).contains(t.getName())){
								this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
							}
						}
					}
				}
			}

			view.checkAndAddLock(l);
		}
	}
	
	//Keep track of variables: If a new variable is seen, add it to variableSet.
	private void checkAndAddVariable(Variable v){
		if(!variableSet.contains(v)){
			variableSet.add(v);
			
			Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
			Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());

			for (Lock l : lockSet){
				if(existsLockReadVariableThreads.containsKey(l.getName())){
					if(existsLockReadVariableThreads.get(l.getName()).containsKey(v.getName())){
						if(!this.lastReleaseLockReadVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockReadVariableThread.get(l).put(vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());	
						}
						if(!this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).put(vEquivWrite, new HashMap<Thread, VectorClock>());	
						}
						HashSet<String> threads = existsLockReadVariableThreads.get(l.getName()).get(v.getName());
						for(Thread t: this.threadToIndex.keySet()){
							if(threads.contains(t.getName())){
								if(!lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t)){
									this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
								}
							}
						}
					}
				}
				
				
				if(existsLockWriteVariableThreads.containsKey(l.getName())){
					if(existsLockWriteVariableThreads.get(l.getName()).containsKey(v.getName())){
						if(!this.lastReleaseLockWriteVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());	
						}
						if(!this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).put(vEquivWrite, new HashMap<Thread, VectorClock>());	
						}
						HashSet<String> threads = existsLockWriteVariableThreads.get(l.getName()).get(v.getName());
						for(Thread t: this.threadToIndex.keySet()){
							if(threads.contains(t.getName())){
								if(!lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t)){
									this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
								}
							}
						}
					}
				}
			}
			
			

			readVariable.put(v, new VectorClock(this.numThreads));
			writeVariable.put(v, new VectorClock(this.numThreads));
			
			HBPredecessorReadVariable.put(v, new VectorClock(this.numThreads));
			HBPredecessorWriteVariable.put(v, new VectorClock(this.numThreads));

		}
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.get(tIndex);
	}
	
	protected VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	//Note that the vector-clock C_t is not explicitly stored. Every time it is needed, it is generated from the integer c_t and the clock P_t.
	//This is an optimization that can save space.
	public VectorClock generateVectorClockFromClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock wcpClock = getVectorClock(WCPThread, t);
		VectorClock pred = getVectorClock(WCPPredecessorThread, t);
		int tValue = getClockThread(t);
		
		wcpClock.copyFrom(pred);
		wcpClock.setClockIndex(tIndex, tValue);
		return wcpClock;
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

	public VectorClock getVectorClock(HashMap<Lock, VectorClock> arr, Lock l) {
		checkAndAddLock(l);
		return arr.get(l);
	}

	public VectorClock getVectorClock(HashMap<Variable, VectorClock> arr, Variable v) {
		checkAndAddVariable(v);
		return arr.get(v);
	}

	public VectorClock getVectorClock(HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>> arr, Lock l, Variable v, Thread t) {
		checkAndAddLock(l);
		checkAndAddVariable(v);
		Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
		Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());	
		if (!arr.containsKey(l) )
			throw new IllegalArgumentException("No l found"); 
		if (!arr.get(l).containsKey(vEquivRead) )
			throw new IllegalArgumentException("No vEquivRead"); 
		if (!arr.get(l).get(vEquivRead).containsKey(vEquivWrite) )
			throw new IllegalArgumentException("No vEquivRead");
		if (!arr.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t) )
			throw new IllegalArgumentException("No t found"); 

		
		return arr.get(l).get(vEquivRead).get(vEquivWrite).get(t);
	}
	
	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.pushClockPair(l, new ClockPair(C_t));
	}
	
	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		local_vc_imax.setToZero();
		VectorClock P_t = getVectorClock(WCPPredecessorThread, t);
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.getMaxLowerBound(t, l, C_t, local_vc_imax);
		P_t.updateWithMax(P_t, local_vc_imax);		
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		VectorClock C_t = generateVectorClockFromClockThread(t);
		VectorClock H_t = getVectorClock(HBPredecessorThread, t);
		
		view.updateTopRelease(l, C_t, H_t);
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
	
	public <T> HashSet<T> stackToSet(Stack<T> stack){
		HashSet<T> set = new HashSet<T>();
		for(T t : stack){
			set.add(t);
		}
		return set;
	}
	
	public HashSet<Lock> getSetFromStack(Thread t){
		return stackToSet(this.mapThreadLockStack.get(t));
	}
	
	public boolean isLockAcquired(Thread t, Lock l){
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		return this.getSetFromStack(t).contains(l);
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}
	
	public void printViewSize(){
		this.view.printSize();
	}
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.lockSet.size()));
		System.err.println("Number of variables = " + Integer.toString(this.variableSet.size()));
		this.view.printSize();
	}
	
	public void destroyLock(Lock l){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete non-existent lock " + l.getName());
		}
		else{
			lockSet.remove(l);

			HBPredecessorLock.remove(l);
			WCPPredecessorLock.remove(l);
			
			lastReleaseLockReadVariableThread.remove(l);
			lastReleaseLockWriteVariableThread.remove(l);
			
			view.destroyLock(l);
		}
	}

	public void destroyLockThreadStack(Lock l, Thread t){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete stacks for non-existent lock " + l.getName());
		}
		else if(!threadToIndex.containsKey(t)){
			throw new IllegalArgumentException("Cannot delete stacks for non-existent thread " + t.getName());
		}
		else if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		else{
			view.destroyLockThreadStack(l, t);
		}
	}
}