package engine.racedetectionengine.goldilocks;

import java.util.HashMap;
import java.util.HashSet;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;

public class GoldilocksState extends State {

	// Data for the algorithm data
	public HashMap<Variable, HashSet<Lock>> writeLockSet;
//	public HashMap<Variable, HashSet<Thread>> writeThreadSet;
	
	public HashMap<Lock, HashMap<Variable, HashSet<Lock>>> readLockSet;
//	public HashMap<Thread, HashMap<Variable, HashSet<Thread>>> readThreadSet;

	public HashMap<Thread, Lock> threadLocks;

	public int verbosity;

	public GoldilocksState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initData(tSet);
	}

	public void initData(HashSet<Thread> tSet) {
		writeLockSet = new HashMap<Variable, HashSet<Lock>> ();
//		writeThreadSet = new HashMap<Variable, HashSet<Thread>> ();
		readLockSet = new HashMap<Lock, HashMap<Variable, HashSet<Lock>>> ();
//		readThreadSet = new HashMap<Thread, HashMap<Variable, HashSet<Thread>>> ();
		
		this.threadLocks = new HashMap<Thread, Lock> ();
		for(Thread t: tSet){
			Lock tLock = new Lock("ThreadLock-" + t.getName());
			this.threadLocks.put(t, tLock);
		}
	}

	@Override
	public void printMemory() {
		System.out.println("Dummy method called");
	}

}