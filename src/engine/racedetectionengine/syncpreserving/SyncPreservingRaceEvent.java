package engine.racedetectionengine.syncpreserving;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Thread;
import event.Variable;
import event.EventType;
import event.Lock;
import util.Pair;
import util.Quintet;
import util.ll.EfficientLLView;
import util.vectorclock.VectorClock;

public class SyncPreservingRaceEvent extends RaceDetectionEvent<SyncPreservingRaceState> {

	static final int flushEventDuration = 100000;
	static int flush_event_ctr = 0;

	@Override
	public boolean Handle(SyncPreservingRaceState state, int verbosity) {

		EventType tp = this.getType();

		// Check if this is a write and the last event was a write
		// on the same variable by the same thread
		// If so, skip everything
		if (tp.isWrite()) {
			if (state.lastType != null) {
				if (state.lastType.isWrite()) {
					if (state.lastDecor == this.getVariable().getId()) {
						if (state.lastThread == this.getThread().getId()) {
							return state.lastAnswer;
						}
					}
				}
			}
		}

		if (tp.isAccessType()) {
			state.checkAndAddVariable(this.getVariable());
		}
		if (tp.isLockType()) {
			state.checkAndAddLock(this.getLock());
		}

		boolean toReturn;
		toReturn = this.HandleSub(state, verbosity);

		state.lastDecor = -1;
		state.lastThread = -1;
		state.lastType = null;
		state.lastAnswer = toReturn;
		if (this.getType().isWrite()) {
			state.lastDecor = this.getVariable().getId();
			state.lastThread = this.getThread().getId();
			state.lastType = tp;
		}

		flush_event_ctr = flush_event_ctr + 1;
		if (flush_event_ctr == flushEventDuration) {
			state.flushAcquireViews();
			flush_event_ctr = 0;
		}

		return toReturn;
	}

	@Override
	public void printRaceInfoLockType(SyncPreservingRaceState state, int verbosity) {
		if (this.getType().isLockType()) {
			if (verbosity >= 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
				str += "C_t = ";
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(SyncPreservingRaceState state, int verbosity) {
		if (this.getType().isAccessType()) {
			if (verbosity >= 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(SyncPreservingRaceState state, int verbosity) {
		if (this.getType().isExtremeType()) {
			if (verbosity >= 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(SyncPreservingRaceState state, int verbosity) {
		Thread t = this.getThread();
		Lock l = this.getLock();

		if (!state.threadsAccessingLocks.containsKey(l)) {
			state.threadsAccessingLocks.put(l, new HashSet<Thread>());
		}
		state.threadsAccessingLocks.get(l).add(t);

		state.numAcquires = state.numAcquires + 1;
		state.incClockThread(getThread());
		state.updateViewAsWriterAtAcquire(l, t);
		this.printRaceInfo(state, verbosity);

		state.addLockHeld(t, l);

		return false;
	}

	@Override
	public boolean HandleSubRelease(SyncPreservingRaceState state, int verbosity) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.incClockThread(t);
		state.updateViewAsWriterAtRelease(l, t);
		state.removeLockHeld(t, l);
		this.printRaceInfo(state, verbosity);

		return false;
	}

	private EventType getEarlierConflictingEvent(SyncPreservingRaceState state, Thread t,
			Variable x, EventType a, Thread u) {
		Pair<VectorClock, Integer> writeTriplet = null;
		int writeClock = 0;
		EfficientLLView<Thread, Pair<VectorClock, Integer>> store_write = state.accessInfo
				.get(u).get(EventType.WRITE).get(x);
		if (!store_write.isEmpty(t)) {
			writeTriplet = store_write.bottom(t);
			writeClock = writeTriplet.second;
		}
		Pair<VectorClock, Integer> readTriplet = null;
		int readClock = 0;
		if (a.equals(EventType.WRITE)) {
			EfficientLLView<Thread, Pair<VectorClock, Integer>> store_read = state.accessInfo
					.get(u).get(EventType.READ).get(x);
			if (!store_read.isEmpty(t)) {
				readTriplet = store_read.bottom(t);
				readClock = readTriplet.second;
			}
		}

		if (writeClock > 0 && readClock > 0) {
			return readClock < writeClock ? EventType.READ : EventType.WRITE;
		} else if (writeClock > 0) {
			return EventType.WRITE;
		} else if (readClock > 0) {
			return EventType.READ;
		} else
			return null;
	}

	private boolean checkRaces(SyncPreservingRaceState state, Thread t, Variable x,
			EventType a, VectorClock C_pred_t) {
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for (Thread u : threadSet_x) {
			if (!u.equals(t)) {

				while (true) {
					EventType aprime = getEarlierConflictingEvent(state, t, x, a, u);
					if (!(aprime == null)) {
						EfficientLLView<Thread, Pair<VectorClock, Integer>> store = state.accessInfo
								.get(u).get(aprime).get(x);
						if (!store.isEmpty(t)) {
							Pair<VectorClock, Integer> conflictingTriplet = store
									.bottom(t);
							VectorClock C_pred_u = conflictingTriplet.first;
							int C_u_u = conflictingTriplet.second;

							// Cheap check
							if (C_u_u <= state.getIndex(C_pred_t, u)) {
								state.flushConflictingEventsEagerly(store, t, a, x, u,
										C_u_u, C_pred_t);
								continue;
							}

							Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey = new Quintet<Thread, EventType, Thread, EventType, Variable>(
									u, aprime, t, a, x);

							VectorClock I = new VectorClock(C_pred_t);
							I.updateWithMax(I, C_pred_u);
							VectorClock lastIeal = state.lastIdeal.get(acquireInfoKey);
							I.updateWithMax(I, lastIeal);
							I.copyFrom(state.fixPointIdeal(acquireInfoKey, I, t));

							state.lastIdeal.put(acquireInfoKey, new VectorClock(I));

							if (!(C_u_u <= state.getIndex(I, u))) {
								return true;
							} else {
								state.flushConflictingEventsEagerly(store, t, a, x, u,
										C_u_u, I);
							}
						}
					} else {
						break;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(SyncPreservingRaceState state, int verbosity) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		EventType tp = this.getType();
		VectorClock C_t = state.getVectorClock(state.clockThread, t);

		// Check race
		VectorClock C_pred_t = new VectorClock(C_t);
		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if (emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t);
		}

		// Update and send clocks
		state.incClockThread(t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		C_t.updateWithMax(C_t, LW_v);
		// Eager computation of closure. Do it after checking for races.
		C_t.copyFrom(state.updatePointersAtAccessAndGetFixPoint(t, C_t));

		Pair<VectorClock, Integer> infoToStore = new Pair<VectorClock, Integer>(C_pred_t,
				state.getIndex(C_t, t));
		state.accessInfo.get(t).get(EventType.READ).get(v).pushTop(infoToStore);

		this.printRaceInfo(state, verbosity);
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SyncPreservingRaceState state, int verbosity) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		EventType tp = this.getType();
		VectorClock C_t = state.getVectorClock(state.clockThread, t);

		// Check race
		VectorClock C_pred_t = new VectorClock(C_t);
		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if (emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t);
		}

		// Do send stuff
		state.incClockThread(t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		LW_v.copyFrom(C_t);
		Pair<VectorClock, Integer> infoToStore = new Pair<VectorClock, Integer>(C_pred_t,
				state.getIndex(C_t, t));
		state.accessInfo.get(t).get(EventType.WRITE).get(v).pushTop(infoToStore);

		this.printRaceInfo(state, verbosity);
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SyncPreservingRaceState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_tc.copyFrom(C_t);
			state.setIndex(C_tc, this.getTarget(), 1);
			this.printRaceInfo(state, verbosity);
			state.incClockThread(getThread());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(SyncPreservingRaceState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state, verbosity);
			state.incClockThread(getThread());
			// Eager computation of closure.
			C_t.copyFrom(
					state.updatePointersAtAccessAndGetFixPoint(this.getThread(), C_t));
		}
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(SyncPreservingRaceState state,
			int verbosity) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean HandleSubBegin(SyncPreservingRaceState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(SyncPreservingRaceState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
