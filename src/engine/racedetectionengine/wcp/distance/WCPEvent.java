package engine.racedetectionengine.wcp.distance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class WCPEvent extends RaceDetectionEvent<WCPState> {

	public WCPEvent() {
		super();
	}

	public boolean Handle(WCPState state, int verbosity) {
		// Do some things first
		if (!(state.mapThreadLockStack.containsKey(this.thread))) {
			state.mapThreadLockStack.put(this.thread, new Stack<Lock>());
			state.mapThreadReadVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
			state.mapThreadWriteVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
		}

		// Now call child's function
		boolean toReturn = this.HandleSub(state, verbosity);
		if (toReturn) {
			state.racyVars.add(this.getVariable().getId());
		}

		return toReturn;
	}

	@Override
	public void printRaceInfoLockType(WCPState state, int verbosity) {
		if (this.getType().isLockType()) {
			if (verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state
						.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(WCPState state, int verbosity) {
		if (this.getType().isAccessType()) {
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getVariable().getName();
			str += "|";
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			str += "|" + Long.toString(this.getAuxId());
			if (verbosity == 1 || verbosity == 2) {
				System.out.println(str);
			}
			if (verbosity == 3) {
				System.out.print(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(WCPState state, int verbosity) {
		if (this.getType().isExtremeType()) {
			if (verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state
						.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(WCPState state, int verbosity) {

		/*** Extra Pre-processing for reEntrants *****/
		boolean reEntrant = state.isLockAcquired(this.getThread(), this.getLock());
		/*** Extra Pre-processing for reEntrants done *****/

		/****** Annotation phase starts **********/
		state.mapThreadLockStack.get(getThread()).push(getLock());
		state.mapThreadReadVarSetStack.get(getThread()).push(new HashSet<Variable>());
		state.mapThreadWriteVarSetStack.get(getThread()).push(new HashSet<Variable>());
		/****** Annotation phase ends **********/

		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
				this.getThread());
		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_t.updateWithMax(H_t, H_l);

		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, getLock());
		P_t.updateWithMax(P_t, P_l);

		if (!reEntrant) {
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(WCPState state, int verbosity) {

		/****** Annotation phase starts **********/
		HashSet<Variable> readVarSet = state.mapThreadReadVarSetStack.get(getThread())
				.pop();
		HashSet<Variable> writeVarSet = state.mapThreadWriteVarSetStack.get(getThread())
				.pop();

		this.setReadVarSet(readVarSet);
		this.setWriteVarSet(writeVarSet);

		state.mapThreadLockStack.get(getThread()).pop();

		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().addAll(readVarSet);
			state.mapThreadWriteVarSetStack.get(getThread()).peek().addAll(writeVarSet);
		}
		/****** Annotation phase ends **********/

		state.readViewOfWriters(getLock(), getThread());

		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, getThread());
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());

		for (Thread tPrime : state.threadToIndex.keySet()) {
			if (tPrime.getId() != this.getThread().getId()) {

				for (Variable r : this.readVarSet) {
					if (state.existsLockReadVariableThreads
							.containsKey(this.getLock().getName())) {
						if (state.existsLockReadVariableThreads
								.get(this.getLock().getName()).containsKey(r.getName())) {
							if (state.existsLockReadVariableThreads
									.get(this.getLock().getName()).get(r.getName())
									.contains(tPrime.getName())) {
								VectorClock L_r_l_x_tprime = state.getVectorClock(
										state.lastReleaseLockReadVariableThread,
										this.getLock(), r, tPrime);
								L_r_l_x_tprime.updateWithMax(L_r_l_x_tprime, H_t, C_t);
							}
						}
					}
				}

				for (Variable w : this.writeVarSet) {
					if (state.existsLockWriteVariableThreads
							.containsKey(this.getLock().getName())) {
						if (state.existsLockWriteVariableThreads
								.get(this.getLock().getName()).containsKey(w.getName())) {
							if (state.existsLockWriteVariableThreads
									.get(this.getLock().getName()).get(w.getName())
									.contains(tPrime.getName())) {
								VectorClock L_w_l_x_tprime = state.getVectorClock(
										state.lastReleaseLockWriteVariableThread,
										this.getLock(), w, tPrime);
								L_w_l_x_tprime.updateWithMax(L_w_l_x_tprime, H_t, C_t);
							}
						}
					}
				}
			}
		}

		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_l.updateWithMax(H_t, C_t);

		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		P_l.copyFrom(P_t);
		state.updateViewAsWriterAtRelease(getLock(), getThread());
		state.incClockThread(getThread());
		this.printRaceInfo(state, verbosity);

		return false;
	}

	@Override
	public boolean HandleSubRead(WCPState state, int verbosity) {
		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
				this.getThread());
		for (Lock l : this.getLockSet()) {
			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						VectorClock writeClock = state.getVectorClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						P_t.updateWithMax(P_t, writeClock);
					}
				}
			}
		}
		/***** P_t updated **************/

		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		long d_min_across_threads_w = 0;
		long d_max_across_threads_w = 0;
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			HashMap<Thread, Long> confAuxIds = state
					.getVectorClock(state.writeVariableAuxId, getVariable());
			d_min_across_threads_w = this.getAuxId()
					- state.getMaxAuxId(W_v, C_t, confAuxIds);
			d_max_across_threads_w = this.getAuxId()
					- state.getMinAuxId(W_v, C_t, confAuxIds);
		}

		if (raceDetected) {
			long d_max = d_max_across_threads_w;
			if (d_max > 0) {
				if (state.maxMaxDistance < d_max) {
					state.maxMaxDistance = d_max;
				}
			}
			state.sumMaxDistance = state.sumMaxDistance + d_max;

			long d_min = d_min_across_threads_w;
			if (d_min > 0) {
				if (state.maxMinDistance < d_min) {
					state.maxMinDistance = d_min;
				}
				state.sumMinDistance = state.sumMinDistance + d_min;
			}
			state.numRaces = state.numRaces + 1;
		}

		if (!state.forceOrder) {
			R_v.updateWithMax(R_v, C_t);
		} else {
			VectorClock RH_v = state.getVectorClock(state.HBPredecessorReadVariable,
					getVariable());
			VectorClock WH_v = state.getVectorClock(state.HBPredecessorWriteVariable,
					getVariable());
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());

			P_t.updateWithMax(P_t, WH_v);
			H_t.updateWithMax(H_t, WH_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state, verbosity);
			R_v.updateWithMax(R_v, C_t);
			RH_v.updateWithMax(RH_v, H_t, C_t);
			state.incClockThread(getThread());
		}

		HashMap<Thread, Long> lastAuxIds = state.getVectorClock(state.readVariableAuxId,
				getVariable());
		lastAuxIds.put(this.getThread(), this.getAuxId());

		return raceDetected;
	}

	private long min_of_nonzero(long a, long b) {
		if (a == 0L)
			return b;
		if (b == 0L)
			return a;
		return a > b ? b : a;
	}

	@Override
	public boolean HandleSubWrite(WCPState state, int verbosity) {

		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadWriteVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
				this.getThread());
		for (Lock l : this.getLockSet()) {

			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						VectorClock writeClock = state.getVectorClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						P_t.updateWithMax(P_t, writeClock);
					}

				}
			}

			if (state.existsLockReadVariableThreads.containsKey(l.getName())) {
				if (state.existsLockReadVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockReadVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						VectorClock readClock = state.getVectorClock(
								state.lastReleaseLockReadVariableThread, l,
								this.getVariable(), this.getThread());
						P_t.updateWithMax(P_t, readClock);
					}

				}
			}
		}
		/***** P_t updated **************/

		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		long d_min_across_threads_r = 0;
		long d_max_across_threads_r = 0;
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			HashMap<Thread, Long> confAuxIds = state
					.getVectorClock(state.readVariableAuxId, getVariable());
			d_min_across_threads_r = this.getAuxId()
					- state.getMaxAuxId(R_v, C_t, confAuxIds);
			d_max_across_threads_r = this.getAuxId()
					- state.getMinAuxId(R_v, C_t, confAuxIds);
		}
		long d_min_across_threads_w = 0;
		long d_max_across_threads_w = 0;
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			HashMap<Thread, Long> confAuxIds = state
					.getVectorClock(state.writeVariableAuxId, getVariable());
			d_min_across_threads_w = this.getAuxId()
					- state.getMaxAuxId(W_v, C_t, confAuxIds);
			d_max_across_threads_w = this.getAuxId()
					- state.getMinAuxId(W_v, C_t, confAuxIds);
		}
		if (raceDetected) {
			long d_max = (d_max_across_threads_r > d_max_across_threads_w)
					? d_max_across_threads_r
					: d_max_across_threads_w;
			if (d_max > 0) {
				if (state.maxMaxDistance < d_max) {
					state.maxMaxDistance = d_max;
				}
			}
			state.sumMaxDistance = state.sumMaxDistance + d_max;

			long d_min = min_of_nonzero(d_min_across_threads_r, d_min_across_threads_w);
			if (d_min > 0) {
				if (state.maxMinDistance < d_min) {
					state.maxMinDistance = d_min;
				}
				state.sumMinDistance = state.sumMinDistance + d_min;
			}
			state.numRaces = state.numRaces + 1;
		}

		if (!state.forceOrder) {
			W_v.updateWithMax(W_v, C_t);
		} else {
			VectorClock RH_v = state.getVectorClock(state.HBPredecessorReadVariable,
					getVariable());
			VectorClock WH_v = state.getVectorClock(state.HBPredecessorWriteVariable,
					getVariable());
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());

			P_t.updateWithMax(P_t, WH_v, RH_v);
			H_t.updateWithMax(H_t, WH_v, RH_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state, verbosity);
			W_v.updateWithMax(W_v, C_t);
			WH_v.updateWithMax(WH_v, H_t, C_t);
			state.incClockThread(getThread());
		}

		HashMap<Thread, Long> lastAuxIds = state.getVectorClock(state.writeVariableAuxId,
				getVariable());
		lastAuxIds.put(this.getThread(), this.getAuxId());

		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(WCPState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());

			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread,
					this.getTarget());
			H_tc.updateWithMax(C_t, H_t);

			VectorClock P_tc = state.getVectorClock(state.WCPPredecessorThread,
					this.getTarget());
			P_tc.updateWithMax(C_t, H_t);

			state.incClockThread(this.getThread());
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(WCPState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {

			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread,
					this.getTarget());
			VectorClock C_tc = state.generateVectorClockFromClockThread(this.getTarget());
			VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
					this.getThread());

			H_t.updateWithMax(H_t, H_tc, C_tc);
			P_t.updateWithMax(P_t, H_tc, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(WCPState state, int verbosity) {
	}

	@Override
	public boolean HandleSubBegin(WCPState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(WCPState state, int verbosity) {
		return false;
	}

}
