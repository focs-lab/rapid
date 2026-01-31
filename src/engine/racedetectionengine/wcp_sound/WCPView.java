package engine.racedetectionengine.wcp_sound;

import event.Lock;
import event.Thread;
import util.Pair;
import util.ll.EfficientLLViewWCP;
import util.vectorclock.ClockEpochPair;
import util.vectorclock.VectorClock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

public class WCPView {

	public static Function<VectorClock, Function<ClockEpochPair, Boolean>> compareEpochVC = vc -> clep -> clep
			.getAcquire().isLessThanOrEqual(vc);

	// private HashMap<Lock, HashMap<Thread, EfficientStore>> view; // (LOCK, index
	// of WRITER) -> Store
	private HashMap<Lock, EfficientLLViewWCP<Thread, ClockEpochPair>> view;
	private HashSet<Thread> threadSet;
	private HashSet<Lock> lockSet;

	public HashMap<String, HashMap<String, Long>> lockThreadLastInteraction;

	// private VectorClock tempMin;

	WCPView(HashSet<Thread> tSet) {
		this.threadSet = new HashSet<>(tSet);
		this.lockSet = new HashSet<>();
		this.view = new HashMap<>();
	}

	public void checkAndAddLock(Lock l) {
		if (!lockSet.contains(l)) {
			lockSet.add(l);
			HashSet<Thread> tSet = new HashSet<Thread>();
			for (Thread tReader : this.threadSet) {
				if (this.lockThreadLastInteraction.get(l.getName())
						.containsKey(tReader.getName())) {
					tSet.add(tReader);
				}
			}
			this.view.put(l, new EfficientLLViewWCP<>(tSet));
		}
	}

	public void pushClockPair(Lock l, ClockEpochPair clockPair) {
		checkAndAddLock(l);
		this.view.get(l).pushTop(clockPair);
	}

	// result is going to be overwritten with the release of the largest acq <= ct
	public VectorClock getMaxLowerBound(Thread tReader, Lock l, VectorClock ct) {
		checkAndAddLock(l);
		Pair<Boolean, ClockEpochPair> mlb = view.get(l).getMaxLowerBound(tReader, compareEpochVC.apply(ct));
		// If the active critical section gets popped out, then put it back
		if (view.get(l).isEmpty()) {
			this.pushClockPair(l, mlb.second);
		}
		if (mlb.first) {
			return mlb.second.getRelease();
		} else {
			return new VectorClock(ct.getDim());
		}
	}

	public void updateTopRelease(Lock l, VectorClock ht) {
		checkAndAddLock(l);
		ClockEpochPair clockPair = this.view.get(l).top();
		clockPair.getRelease().copyFrom(ht);
	}

	public String toString() {
		String str = "";
		for (Lock l : lockSet) {
			str += "[" + l.getName() + "]";
			str += " : " + view.get(l).toString() + "\n";
		}
		str += "\n";
		return str;
	}

	public int getSize() {
		int sz = 0;
		for (Lock l : lockSet) {
			sz += view.get(l).getLength();
		}
		return sz;
	}

	public void printSize() {
		System.err.println("Stack size = " + Integer.toString(this.getSize()));
	}

	public void destroyLock(Lock l) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete non-existent lock " + l.getName());
		} else {
			lockSet.remove(l);
			view.remove(l);
		}
	}

	public void destroyLockThreadStack(Lock l, Thread t) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete stack for non-existent lock " + l.getName());
		} else if (!threadSet.contains(t)) {
			throw new IllegalArgumentException(
					"Cannot delete stack for non-existent thread " + t.getName());
		} else {
			if (this.lockThreadLastInteraction.get(l.getName())
					.containsKey(t.getName())) {
				this.view.get(l).destroyKey(t);
				this.lockThreadLastInteraction.get(l.getName()).remove(t.getName());
			}

		}
	}

}