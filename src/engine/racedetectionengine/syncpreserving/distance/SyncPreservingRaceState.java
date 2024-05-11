package engine.racedetectionengine.syncpreserving.distance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.Quintet;
import util.Triplet;
import util.TripletComparators;
import util.ll.EfficientLLView;
import util.ll.EfficientNode;
import util.vectorclock.VectorClock;

public class SyncPreservingRaceState extends State {

	public static EventType[] accessTypes =  {EventType.READ, EventType.WRITE};

	// == Internal data ==
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;
	private HashMap<Thread, HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>>> threadPairToAcquireInfoKeys;
	private HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> secondThreadToAcquireInfoKeys; // t -> {< t1 , a1, t, a2, x_> | t1, a1, a2, x}
	private HashMap<Thread, Quintet<Thread, EventType, Thread, EventType, Variable>> threadToDummyKey;
	private HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> acquireInfoKeys;


	// == Data used for algorithm ==

	// 0. ThreadSet
	public HashSet<Thread> threads;
	public HashSet<Lock> locks;
	public HashSet<Variable> variables;

	// 1. Vector clocks
	public ArrayList<VectorClock> clockThread; // threadIndex -> VC
	public ArrayList<VectorClock> lastWriteVariable; // variableIndex -> VC
	public HashMap<Quintet<Thread, EventType, Thread, EventType, Variable>, VectorClock> lastIdeal;

	// 2. Scalars
	public long numAcquires; // counts the total number of acquire events seen

	// 3. Views
	public HashMap<Thread, HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>>>>> accessInfo;
	public HashMap<Thread, HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>> acquireInfo;
	//	public HashMap<Thread, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Pair<Integer, HashSet<Lock>>>> openLockInfo;
	// == End of data used for algorithm ==

	public TripletComparators.FirstComparator<Integer, Long, VectorClock> firstComparatorAcquireInfo;
	public TripletComparators.FirstComparator<VectorClock, Integer, Long> firstComparatorAccessInfo;
	public TripletComparators.SecondComparator<VectorClock, Integer, Long> secondComparatorAccessInfo;
	public Pair<VectorClock, VectorClock> bottomVCTriplet;

	// Lockset optimization
	//	public HashMap<Thread, HashMap<Lock, Integer>> threadToLockDepth;
	public HashMap<Thread, HashSet<Lock>> threadToLocksHeld;
	public HashMap<Variable, HashSet<Lock>> variableToLockset;
	private Lock readLock;
	private HashMap<Thread, Lock> threadLock;

	// Reducing number of pointers
	public HashMap<String, HashSet<String>> stringVariableToThreadSet; // name(x) -> set of thread-names that access x
	public HashMap<Variable, HashSet<Thread>> variableToThreadSet; // x -> set of threads that access x
	public HashMap<Lock, HashSet<Thread>> threadsAccessingLocks;

	// fastpath
	public int lastThread = -1;
	public int lastDecor = -1;
	public EventType lastType = null;
	public boolean lastAnswer = false;

	// == stats ==
	//	public long numIters = 0;
	public long maxDistance = 0;
	public long sumDistance = 0;
	public long numRaces = 0;
	public HashSet<Integer> racyVars = new HashSet<Integer> ();

	// == parameter flags ==
	public boolean forceOrder;

	public SyncPreservingRaceState(HashSet<Thread> tSet) {
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

		this.readLock = new Lock("__READ-LOCK__");
		this.threadLock = new HashMap<Thread, Lock> ();
		for(Thread t: tSet) {
			Lock tLock = new Lock("__Thred-" + t.getName() + "-LOCK__");
			this.threadLock.put(t,  tLock);
		}
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {

		this.threads = new HashSet<Thread> (tSet);
		this.locks = new HashSet<Lock> ();
		this.variables = new HashSet<Variable> ();

		this.secondThreadToAcquireInfoKeys = new HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> ();
		//		this.dummyVar = new Variable();
		this.threadToDummyKey = new HashMap<Thread, Quintet<Thread, EventType, Thread, EventType, Variable>> ();
		this.threadPairToAcquireInfoKeys = new HashMap<Thread, HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>>> ();
		for(Thread t1: tSet) {
			this.threadToDummyKey.put(t1, new Quintet<Thread, EventType, Thread, EventType, Variable> (t1, null, null, null, null));
			this.secondThreadToAcquireInfoKeys.put(t1,  new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> ());
			HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> acqInfo_t1 = new HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> ();
			this.threadPairToAcquireInfoKeys.put(t1, acqInfo_t1);
			for(Thread t2: tSet) {
				if(t1.equals(t2)) {
					continue;
				}
				this.threadPairToAcquireInfoKeys.get(t1).put(t2,  new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> ());
			}
		}
		this.acquireInfoKeys = new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> (this.threadToDummyKey.values());

		// initialize clockThread
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);		
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClock>();

		// initialize lastIdeal
		this.lastIdeal = new HashMap<Quintet<Thread, EventType, Thread, EventType, Variable>, VectorClock> ();

		// initialize numAcquires
		this.numAcquires = 0L;

		// initialize ReadInfo and WriteInfo
		this.accessInfo = new HashMap<Thread, HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>>>>> ();
		for (Thread t: tSet) {
			this.accessInfo.put(t, new HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>>>> ());
			this.accessInfo.get(t).put(EventType.READ, new HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>>> ());
			this.accessInfo.get(t).put(EventType.WRITE, new HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>>> ());			
		}

		// initialize AcquireInfo
		this.acquireInfo = new HashMap<Thread, HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>> ();
		for(Thread t: tSet) {
			this.acquireInfo.put(t,  new HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>());
		}

		//		this.openLockInfo = new HashMap<Thread, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Pair<Integer, HashSet<Lock>>>> ();
		//		for(Thread t: tSet) {
		//			this.openLockInfo.put(t,  new EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Pair<Integer, HashSet<Lock>>> (new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>()));
		//		}

		firstComparatorAcquireInfo = new TripletComparators.FirstComparator<Integer, Long, VectorClock> ();
		firstComparatorAccessInfo = new TripletComparators.FirstComparator<VectorClock, Integer, Long> ();
		secondComparatorAccessInfo = new TripletComparators.SecondComparator<VectorClock, Integer, Long> ();
		bottomVCTriplet = new Pair<VectorClock, VectorClock> (new VectorClock(this.numThreads), new VectorClock(this.numThreads));

		//		this.threadToLockDepth = new HashMap<Thread, HashMap<Lock, Integer>> ();
		this.threadToLocksHeld = new HashMap<Thread, HashSet<Lock>> ();
		for(Thread t: tSet) {
			//			this.threadToLockDepth.put(t,  new HashMap<Lock, Integer> ());
			this.threadToLocksHeld.put(t,  new HashSet<Lock> ());
		}
		this.variableToLockset = new HashMap<Variable, HashSet<Lock>> ();

		this.stringVariableToThreadSet = null;
		this.variableToThreadSet = new HashMap<Variable, HashSet<Thread>> ();
		this.threadsAccessingLocks = new HashMap<Lock, HashSet<Thread>> ();
	}

	// Access methods
	private static <E> E getElementFrom1DArray(ArrayList<E> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			locks.add(l);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			for(Thread t: threadToIndex.keySet()) {
				this.acquireInfo.get(t).put(l, new EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> (this.acquireInfoKeys));
			}
		}
		return lockToIndex.get(l);
	}

	public int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variables.add(v);
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.lastWriteVariable.add(new VectorClock(this.numThreads));			

			HashSet<Thread> threadsAccessingVar = new HashSet<Thread> ();
			if(this.stringVariableToThreadSet == null) {
				this.variableToThreadSet.put(v, this.threads);
			}
			else if(!this.stringVariableToThreadSet.containsKey(v.getName())) {
				this.variableToThreadSet.put(v, threadsAccessingVar);
			}
			else {
				HashSet<String> stringthreadsAccessingVar = this.stringVariableToThreadSet.get(v.getName());
				for(Thread t: this.threads) {
					if (stringthreadsAccessingVar.contains(t.getName())) {
						threadsAccessingVar.add(t);
					}
				}
				this.variableToThreadSet.put(v, threadsAccessingVar);
			}


			for(Thread t: threadToIndex.keySet()) {
				if(!threadsAccessingVar.contains(t)) {
					continue;
				}
				this.accessInfo.get(t).get(EventType.READ).put(v, new EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> (threadsAccessingVar));
				this.accessInfo.get(t).get(EventType.WRITE).put(v, new EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> (threadsAccessingVar));
				for(Thread u: threadToIndex.keySet()) {
					if(u.equals(t)) {
						continue;
					}

					if(!threadsAccessingVar.contains(u)) {
						continue;
					}

					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_read_write = new Quintet<Thread, EventType, Thread, EventType, Variable> (u, EventType.READ, t, EventType.WRITE, v);
					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_write_read = new Quintet<Thread, EventType, Thread, EventType, Variable> (u, EventType.WRITE, t, EventType.READ, v);
					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_write_write = new Quintet<Thread, EventType, Thread, EventType, Variable> (u, EventType.WRITE, t, EventType.WRITE, v);

					HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> otherKeys = this.threadPairToAcquireInfoKeys.get(u).get(t);			
					for(Thread s: this.threads){
						//						this.openLockInfo.get(s).addKeyToBottom(new_key_read_write);
						//						this.openLockInfo.get(s).addKeyToBottom(new_key_write_read);
						//						this.openLockInfo.get(s).addKeyToBottom(new_key_write_write);
						for(Lock l: this.locks) {
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_read_write, otherKeys);
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_write_read, otherKeys);
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_write_write, otherKeys);
						}
					}

					this.acquireInfoKeys.add(new_key_read_write);
					this.acquireInfoKeys.add(new_key_write_read);
					this.acquireInfoKeys.add(new_key_write_write);
					this.threadPairToAcquireInfoKeys.get(u).get(t).add(new_key_read_write);
					this.threadPairToAcquireInfoKeys.get(u).get(t).add(new_key_write_read);
					this.threadPairToAcquireInfoKeys.get(u).get(t).add(new_key_write_write);
					this.secondThreadToAcquireInfoKeys.get(t).add(new_key_read_write);
					this.secondThreadToAcquireInfoKeys.get(t).add(new_key_write_read);
					this.secondThreadToAcquireInfoKeys.get(t).add(new_key_write_write);

					this.lastIdeal.put(new_key_read_write, new VectorClock(this.numThreads));
					this.lastIdeal.put(new_key_write_read, new VectorClock(this.numThreads));
					this.lastIdeal.put(new_key_write_write, new VectorClock(this.numThreads));
				}
			}
			this.variableToLockset.put(v, null);
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
		return getElementFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getElementFrom1DArray(arr, lIndex);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getElementFrom1DArray(arr, vIndex);
	}

	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public void addLockHeld(Thread t, Lock l) {
		//		HashMap<Lock, Integer> tLocks = this.threadToLockDepth.get(t);
		//		if(!tLocks.containsKey(l)) {
		//			tLocks.put(l, 0); 
		//		}
		//		tLocks.put(l, tLocks.get(l) + 1);
		this.threadToLocksHeld.get(t).add(l);
	}

	public void removeLockHeld(Thread t, Lock l) {
		//		HashMap<Lock, Integer> tLocks = this.threadToLockDepth.get(t);
		//		if(!tLocks.containsKey(l)) {
		//			throw new IllegalArgumentException("Lock " + l + "released by thread " + t + " without being acquired");
		//		}
		//		int tl_depth = tLocks.get(l);
		//		if(tl_depth <= 0) {
		//			throw new IllegalArgumentException("Lock " + l + "released by thread " + t + " without being acquired");
		//		}
		//		tLocks.put(l, tl_depth - 1);
		//		if(tl_depth == 1) {
		//			this.threadToLocksHeld.get(t).remove(l);
		//		}
		this.threadToLocksHeld.get(t).remove(l);
	}

	public boolean updateLocksetAtAccess(Thread t, Variable x, EventType tp) {
		//		this.checkAndAddVariable(x);
		HashSet<Lock> vSet = this.variableToLockset.get(x);
		HashSet<Lock> lockset = new HashSet<Lock> ();
		if(tp.isRead()) {
			lockset.add(this.readLock);
		}
		lockset.add(this.threadLock.get(t));
		lockset.addAll(this.threadToLocksHeld.get(t));
		if(vSet == null) {
			this.variableToLockset.put(x, lockset);
		}
		else {
			vSet.retainAll(lockset);
		}		
		return this.variableToLockset.get(x).isEmpty();
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		//		checkAndAddLock(l);
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = this.clockThread.get(tIndex);
		int n = C_t.getClockIndex(tIndex);
		long m = this.numAcquires;
		acquireInfo.get(t).get(l).pushTop(new Triplet<Integer, Long, VectorClock>(n, m, null));
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t_copy = new VectorClock(this.clockThread.get(tIndex));
		Triplet<Integer, Long, VectorClock> info = acquireInfo.get(t).get(l).top();
		Triplet<Integer, Long, VectorClock> new_info = new Triplet<Integer, Long, VectorClock>(info.first, info.second, C_t_copy);
		acquireInfo.get(t).get(l).setTop(new_info);
	}

	public void flushAcquireViews() {
		for(HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>> l_to_store: this.acquireInfo.values()) {
			for(EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> store: l_to_store.values()) {
				store.flush();
			}
		}
	}

	// Modifies I_old
	// If t is not null, then advance acquireInfoKey to dummyKey(t). Otherwise use acquireKey as is.
	public VectorClock fixPointIdeal(Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey, VectorClock I_old, Thread t) {
		VectorClock I = new VectorClock(I_old);
		//		this.numIters = 0;
		boolean first_iter = true;
		while(true) {
			//			this.numIters = this.numIters + 1;
			HashSet<Thread> threads_in_I = new HashSet<Thread> ();
			HashMap<Thread, Triplet<Integer, Long, VectorClock>> base_triplets = new HashMap<Thread, Triplet<Integer, Long, VectorClock>> ();
			for(Thread v: this.threads) {
				int I_v = this.getIndex(I, v);
				if(I_v > 0) {
					threads_in_I.add(v);
					Triplet<Integer, Long, VectorClock> triplet_I_v = new Triplet<Integer, Long, VectorClock> (I_v, 0L, null);
					base_triplets.put(v, triplet_I_v);
				}
			}
			//			HashSet<Lock> openLocks = state.openLocksInIdeal(I, acquireInfoKey);
			//			for(Lock l: openLocks) {
			for(Lock l: this.threadsAccessingLocks.keySet()) {
				long LA_l = -1;
				VectorClock maxVC_match_l = null;
				Thread max_thread = null;
				Pair<EfficientNode<Triplet<Integer, Long, VectorClock>>, Integer> max_nextNode = null;
				HashSet<Thread> threads_accessing_l_and_in_I = new HashSet<Thread> (this.threadsAccessingLocks.get(l));
				threads_accessing_l_and_in_I.retainAll(threads_in_I);
				for(Thread v: threads_accessing_l_and_in_I) {
					EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> store = this.acquireInfo.get(v).get(l);
					// if store is empty then skip
					if (store.isEmpty()) {
						this.threadsAccessingLocks.get(l).remove(v);
						if(this.threadsAccessingLocks.get(l).isEmpty()) {
							this.threadsAccessingLocks.remove(l);
						}
						continue;
					}
					
					// read the value of dummyKey(t)
					if(first_iter) {
						if(t != null) {
							Quintet<Thread, EventType, Thread, EventType, Variable> dummyKey = this.threadToDummyKey.get(t);
							store.advanceKeyToTarget(acquireInfoKey, dummyKey);
						}
					}
					
					if(store.isEmpty(acquireInfoKey)) {
						continue;
					}
					
					Triplet<Integer, Long, VectorClock> bottomPointer = store.bottom(acquireInfoKey);
					if(bottomPointer.first > this.getIndex(I, v)) {
						continue;
					}

					// change the following to With Update
					Triplet<Boolean, Triplet<Integer, Long, VectorClock>, Pair<EfficientNode<Triplet<Integer, Long, VectorClock>>, Integer>> found_lockTriplet_nextNodeIter = store.getMaxLowerBoundPenultimate(acquireInfoKey, base_triplets.get(v), this.firstComparatorAcquireInfo);
					if(found_lockTriplet_nextNodeIter.first) {
						Triplet<Integer, Long, VectorClock> lockTriplet = found_lockTriplet_nextNodeIter.second;
						long GI_v_l = lockTriplet.second;
						VectorClock C_match_v_l = lockTriplet.third;
						if(LA_l == -1) {
							LA_l = GI_v_l;
							maxVC_match_l = C_match_v_l;
							max_thread = v;
							max_nextNode = found_lockTriplet_nextNodeIter.third;
						}
						else {
							if(GI_v_l > LA_l) {
								I.updateWithMax(I, maxVC_match_l);
								this.acquireInfo.get(max_thread).get(l).setBottom(acquireInfoKey, max_nextNode);

								LA_l = GI_v_l;
								maxVC_match_l = C_match_v_l;
								max_thread = v;
								max_nextNode = found_lockTriplet_nextNodeIter.third;
							}
							else {								
								I.updateWithMax(I, C_match_v_l);
								this.acquireInfo.get(v).get(l).setBottom(acquireInfoKey, found_lockTriplet_nextNodeIter.third);
							}
						}
					}
					// TODO: If you are not clearing the store, it cannot become empty at this point.
					//					if (store.isEmpty()) {
					//						state.threadsAccessingLocks.get(l).remove(v);
					//						if(state.threadsAccessingLocks.get(l).isEmpty()) {
					//							state.threadsAccessingLocks.remove(l);
					//						}
					//					}
				}
			}
			if(I.isEqual(I_old)) {
				break;
			}
			I_old.copyFrom(I);
			first_iter = false;
		}
		return I;
	}

	// Remove all events e of thread v, with clock C and pred_clock P such that
	// lb_local_clock <= P[u] and C[v] <= I[v]
	private void clearViews(Thread t, EventType a, Variable x, Thread u, int lb_local_clock, VectorClock ub_clock) {
		VectorClock lb_clock = new VectorClock(this.threads.size());
		this.setIndex(lb_clock, u, lb_local_clock);
		Triplet<VectorClock, Integer, Long> lb = new Triplet<VectorClock, Integer, Long> (lb_clock, -1, null);
		HashSet<Thread> threadSet_x = this.variableToThreadSet.get(x);
		for(Thread v: threadSet_x) {
			if(v.equals(t)) {
				continue;
			}
			int ub_local_clock = this.getIndex(ub_clock, v);
			Triplet<VectorClock, Integer, Long> ub = new Triplet<VectorClock, Integer, Long> (null, ub_local_clock, null);
			for(EventType aprime: SyncPreservingRaceState.accessTypes) {
				if(EventType.conflicting(a, aprime)) {
					this.accessInfo.get(v).get(aprime).get(x).removePrefixWithinReturnMin(t, lb, this.firstComparatorAccessInfo, ub, this.secondComparatorAccessInfo);
				}
			}
		}
	}

	// Remove all events that cannot be in race with e2
	public void flushConflictingEventsEagerly(EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> store, Thread t, EventType a, Variable x, Thread u, int lb_local_clock, VectorClock ub_clock) {
		store.advanceKeyByOne(t);
		this.clearViews(t, a, x, u, lb_local_clock, ub_clock);
	}

	/*
	HashSet<Lock> openLocksInIdeal(VectorClock V, Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey){
		HashSet<Lock> openLocks = new HashSet<Lock> ();
		for(Thread t: this.threads) {
			int local_clock = this.getIndex(V, t);
			Pair<Integer, HashSet<Lock>> searchKey = new Pair<Integer, HashSet<Lock>>(local_clock, null);
			EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Pair<Integer, HashSet<Lock>>> store = this.openLockInfo.get(t);
			Triplet<Boolean, Pair<Integer, HashSet<Lock>>, Pair<EfficientNode<Pair<Integer, HashSet<Lock>>>, Integer>> found_openLockInfo_nextNodeIter = store.getMaxLowerBoundPenultimate(acquireInfoKey, searchKey, this.firstComparatorOpenLockInfo);
			if(found_openLockInfo_nextNodeIter.first) {
				Pair<Integer, HashSet<Lock>> openLockInfo = found_openLockInfo_nextNodeIter.second;
				int found_local_clock = openLockInfo.first;
				if(found_local_clock == local_clock) {
					openLocks.addAll(openLockInfo.second);
				}
			}
		}
		return openLocks;
	}
	 */

	public VectorClock updatePointersAtAccessAndGetFixPoint(Thread t, VectorClock I) {
		Quintet<Thread, EventType, Thread, EventType, Variable> t_key = this.threadToDummyKey.get(t);
		VectorClock new_I = fixPointIdeal(t_key, I, null);

		// Now set all other keys to the same pointer as t_key
//		HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> keys = this.secondThreadToAcquireInfoKeys.get(t);
//		for(Lock l: this.threadsAccessingLocks.keySet()) {
//			for(Thread v: this.threadsAccessingLocks.get(l)) {
//				EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> store_v_l = this.acquireInfo.get(v).get(l);
//
//				store_v_l.setBottomOfAllKeysToGivenKey(keys, t_key);
//			}
//		}
		return new_I;
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

	//	public boolean isReEntrant(Thread t, Lock l) {
	//		if(this.threadToLockDepth.containsKey(t)) {
	//			if(this.threadToLockDepth.get(t).containsKey(l)) {
	//				if(this.threadToLockDepth.get(t).get(l) > 0) {
	//					return true;
	//				}
	//			}
	//		}
	//		return false;
	//	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}

	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}

}