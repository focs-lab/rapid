package engine.atomicity.conflictserializability.velodome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.Transaction;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.BreadthFirstIterator;
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
	
	public HashMap<Thread, Transaction> threadToCurrentTransaction; // mathcal{C}
	public HashMap<Thread, Transaction> threadToLastOpTransaction;// mathcal{L}
	public HashMap<Lock, Transaction> lockToLastReleaseTransaction;// mathcal{U}
	public HashMap<Variable, HashMap<Thread, Transaction>> readVariableThreadToTransaction;// mathcal{R}
	public HashMap<Variable, Transaction> writeVariableToTransaction;// mathcal{W}
	public Graph<Integer, DefaultEdge> thb_graph; //mathcal{H}
	public HashMap<Thread, Integer> threadToNestingDepth;
	public CycleDetector<Integer, DefaultEdge> cycleDetector_thb_graph;
	
	//Handling forks and joins
	public HashSet<Thread> startedThreads;
	public HashMap<Thread, Transaction> parentTransaction;
	
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
		this.cycleDetector_thb_graph = new CycleDetector<Integer, DefaultEdge>(this.thb_graph);
		
		this.threadToNestingDepth = new HashMap<Thread, Integer> ();
		for(Thread t: tSet){
			this.threadToNestingDepth.put(t, 0);
		}
		
		this.startedThreads = new HashSet<Thread> ();
		this.parentTransaction = new HashMap<Thread, Transaction> ();
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

	public void garbageCollect(Transaction n) {
		// invoked only when the transaction ends so no need to check if n \not\in Range(C)
		boolean no_incoming_edge = false;
		
		if(!this.thb_graph.containsVertex(n.id)) no_incoming_edge = true;
		else {
			if(thb_graph.inDegreeOf(n.id) == 0) {
				no_incoming_edge = true;
				Set<DefaultEdge> edges_of_n = thb_graph.edgesOf(n.id);
				thb_graph.removeAllEdges(edges_of_n);
				thb_graph.removeVertex(n.id);
			}
		}
		
		if(no_incoming_edge) {
			for(Thread t: this.threadToLastOpTransaction.keySet()) {
				if(this.threadToLastOpTransaction.get(t).equals(n)) {
					this.threadToLastOpTransaction.put(t,  this.getBottomTransaction());
				}
			}
			for(Lock l: this.lockToLastReleaseTransaction.keySet()) {
				if(this.lockToLastReleaseTransaction.get(l).equals(n)) {
					this.lockToLastReleaseTransaction.put(l,  this.getBottomTransaction());
				}
			}
			for(Variable v: this.readVariableThreadToTransaction.keySet()) {
				for(Thread t: this.readVariableThreadToTransaction.get(v).keySet()) {
					if(this.readVariableThreadToTransaction.get(v).get(t).equals(n)) {
						this.readVariableThreadToTransaction.get(v).put(t,  this.getBottomTransaction());
					}
				}
			}
			for(Variable v: this.writeVariableToTransaction.keySet()) {
				if(this.writeVariableToTransaction.get(v).equals(n)) {
					this.writeVariableToTransaction.put(v,  this.getBottomTransaction());
				}
			}
		}
	}
	
	private boolean pathExists(int src, int tgt) {
		boolean exists = false;
		BreadthFirstIterator<Integer, DefaultEdge> bfs_iter = new BreadthFirstIterator<Integer, DefaultEdge>(this.thb_graph, src);
        while (bfs_iter.hasNext()) {
            int vtx = bfs_iter.next();
            if (vtx == tgt) {
            	exists = true;
                break;
            }
        }
		return exists;
	}
	
	//Performs merge and checks for cycle in the resulting graph.
	public Pair<Transaction, Boolean> mergeAndCheckCycle(Thread t, HashSet<Transaction> txn_set) {
		if(txn_set.isEmpty()) throw new IllegalArgumentException("Expecting non-empty set of transactions in method 'mergeAndCheckCycle'");
		
		Transaction return_tr = null;
		boolean has_cycle = false;
		
		boolean all_bottom = true;
		for(Transaction tr : txn_set) {
			if(!VelodromeState.isBottomTransaction(tr)) {
				all_bottom = false;
			}
		}
		
		if(all_bottom) {
			return_tr = this.getBottomTransaction();
		}
		else {
			Transaction confirmed_sink = null;
			boolean sink_confirmed = false;
			for(Transaction potential_sink: txn_set) {
				if(!VelodromeState.isBottomTransaction(potential_sink)) {
					boolean path_from_all = true;
					for(Transaction node: txn_set) {
						if(!VelodromeState.isBottomTransaction(node)) {
							if(node != potential_sink) {
								if(!pathExists(node.id, potential_sink.id)) {
									path_from_all = false;
									break;
								}
							}
						}	
					}
					if(path_from_all) {
						confirmed_sink = potential_sink;
						sink_confirmed = true;
						break;
					}
				}
			}
			if(sink_confirmed && confirmed_sink.thread.equals(t)) {
				return_tr = confirmed_sink;
			}
			else {
				return_tr = this.getFreshTransaction(t);
				
				for(Transaction tr : txn_set) {
					if(!VelodromeState.isBottomTransaction(tr)) {
						has_cycle = has_cycle || specialUnionAndCheckCycle(tr, return_tr);
						if(has_cycle) break;
					}
				}
				
			}
		} 
		
		return new Pair<Transaction, Boolean> (return_tr, has_cycle);
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

	public int numTransactionsActive() {
		return this.thb_graph.vertexSet().size();
	}
}