package engine.racedetectionengine.lockset;

import java.util.HashMap;
import java.util.HashSet;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;

public class LockSetState extends State {

	// Data for the algorithm data
	public HashMap<Thread, HashMap<Lock, Integer>> locksHeldNesting;
	public HashMap<Thread, HashSet<Lock>> locksHeldSet;
	public HashMap<Variable, HashSet<Lock>> lockSet;
	public Lock dummyReadLock;
	public HashMap<Thread, Lock> threadLocks;

	public int verbosity;

	public LockSetState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initData(tSet);
	}

	public void initData(HashSet<Thread> tSet) {
		this.dummyReadLock = new Lock("dummy-read-lock");
		this.threadLocks = new HashMap<Thread, Lock> ();
		this.locksHeldNesting = new HashMap<Thread, HashMap<Lock, Integer>> ();
		this.locksHeldSet = new HashMap<Thread, HashSet<Lock>> ();
		for(Thread t: tSet){
			Lock tLock = new Lock("ThreadLock-" + t.getName());
			this.threadLocks.put(t, tLock);
			this.locksHeldNesting.put(t, new HashMap<Lock, Integer> ());
			this.locksHeldNesting.get(t).put(tLock, 1);
			this.locksHeldSet.put(t,  new HashSet<Lock> ());
			this.locksHeldSet.get(t).add(tLock);
		}
		lockSet = new HashMap<Variable, HashSet<Lock>> ();
	}

	@Override
	public void printMemory() {
		System.out.println("Dummy method called");
	}

}