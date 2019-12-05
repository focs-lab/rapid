package engine.atomicity.conflictserializability.velodome_basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Transaction;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.cycle.CycleDetector;

public class VelodromeState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;
	
	private int transaction_ctr;
	private static Transaction bottom_transaction = new Transaction();
	
//	public HashMap<Thread, Stack<Integer>> transactionStackThread; // t -> Stack(transaction_id)
	public HashMap<Thread, Transaction> threadToCurrentTransaction; // mathcal{C}
	public HashMap<Thread, Transaction> threadToLastOpTransaction;// mathcal{L}
	public HashMap<Lock, Transaction> lockToLastReleaseTransaction;// mathcal{U}
	public HashMap<Variable, HashMap<Thread, Transaction>> readVariableThreadToTransaction;// mathcal{R}
	public HashMap<Variable, Transaction> writeVariableToTransaction;// mathcal{W}
	public Graph<Integer, DefaultEdge> thb_graph; //mathcal{H}
	public CycleDetector<Integer, DefaultEdge> cycleDetector_thb_graph;
	
	//parameter flags
	public int verbosity;

	public VelodromeState(HashSet<Thread> tSet, int verbosity) {
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
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}
		
		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
		this.transaction_ctr = 0;
	}

	public void initData(HashSet<Thread> tSet) {
		this.threadToCurrentTransaction = new HashMap<Thread, Transaction>();
		for(Thread t: tSet){
			this.threadToCurrentTransaction.put(t, bottom_transaction);
		}
		
		this.threadToLastOpTransaction = new HashMap<Thread, Transaction> ();
		for(Thread t: tSet){
			this.threadToLastOpTransaction.put(t, bottom_transaction);
		}
		
		this.lockToLastReleaseTransaction = new HashMap<Lock, Transaction> ();
		
		this.readVariableThreadToTransaction = new HashMap<Variable, HashMap<Thread, Transaction>>();
		
		this.writeVariableToTransaction = new HashMap<Variable, Transaction> ();
		
		this.thb_graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		cycleDetector_thb_graph = new CycleDetector<Integer, DefaultEdge>(this.thb_graph);
	}
	
	public Transaction getBottomTransaction(){
		return bottom_transaction;
	}
	
	public static boolean isBottomTransaction(Transaction tr){
		return tr.id == -1;
	}
	
	private int getFreshTransactionId(){
		this.transaction_ctr ++;
		return this.transaction_ctr;
	}
	
	public Transaction getFreshTransaction(Thread t){
		return new Transaction(t, this.getFreshTransactionId());
	}
	
	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			this.lockToLastReleaseTransaction.put(l, bottom_transaction);
		}
		return lockToIndex.get(l);
	}
	
	public int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.readVariableThreadToTransaction.put(v, new HashMap<Thread, Transaction> ());
			for(Thread t: this.threadToIndex.keySet()){
				this.readVariableThreadToTransaction.get(v).put(t,  bottom_transaction);
			}
			this.writeVariableToTransaction.put(v, bottom_transaction);
		}
		return variableToIndex.get(v);
	}
	
	public boolean specialUnionAndCheckCycle(Transaction n1, Transaction n2){
		boolean newCycle = false;
		if(!isBottomTransaction(n1) && !isBottomTransaction(n2) && (n1.id != n2.id)){
			this.thb_graph.addVertex(n1.id);
			this.thb_graph.addVertex(n2.id);
			if(!this.thb_graph.containsEdge(n1.id, n2.id)) {
				this.thb_graph.addEdge(n1.id, n2.id);
				if(cycleDetector_thb_graph.detectCycles()) {
					newCycle = true;
				}
			}
		}
		return newCycle;
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}
	
	public boolean hasNonTrivialCycle(){
		return false;
	}
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}