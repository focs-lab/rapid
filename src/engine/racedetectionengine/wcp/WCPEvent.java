package engine.racedetectionengine.wcp;

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
		// Do some booking
		if (!(state.mapThreadLockStack.containsKey(this.thread))) {
			state.mapThreadLockStack.put(thread, new Stack<Lock>());
			state.mapThreadReadVarSetStack.put(thread, new Stack<HashSet<Variable>>());
			state.mapThreadWriteVarSetStack.put(thread, new Stack<HashSet<Variable>>());
		}

		// Now call the appropriate function according to event type
		return this.HandleSub(state, verbosity);
	}

	/************** Pretty Printing *******************/
	@Override
	public void printRaceInfoLockType(WCPState state, int verbosity) {
		if (verbosity >= 2) {
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getLock().toString();
			str += "|";
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			System.out.println(str);
		}
	}

	@Override
	public void printRaceInfoAccessType(WCPState state, int verbosity) {
		if (verbosity >= 2) {
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
			System.out.println(str);
		}
	}

	@Override
	public void printRaceInfoExtremeType(WCPState state, int verbosity) {
		if (verbosity >= 2) {
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getTarget().toString();
			str += "|";
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			System.out.println(str);
		}
	}

	/************************************************/

	/************** Acquire/Release *******************/
	// Handler for acquire event
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
		H_t.updateWithMax(H_t, H_l); // Line 1 of algorithm

		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, getLock());
		P_t.updateWithMax(P_t, P_l); // Line 2 of algorithm

		// No need to update the queue(s) for re-entrant lock
		if (!reEntrant) {
			state.updateViewAsWriterAtAcquire(getLock(), getThread()); // Line 3 of
																		// algorithm
		}

		this.printRaceInfo(state, verbosity);

		return false;
	}

	// Handler for Release event
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

		state.readViewOfWriters(getLock(), getThread()); // Lines 4-6 of algorithm

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
								L_r_l_x_tprime.updateWithMax(L_r_l_x_tprime, H_t, C_t); // Body
																						// of
																						// loop
																						// at
																						// Line
																						// 7
																						// of
																						// algorithm.
																						// This
																						// is
																						// slightly
																						// different
																						// from
																						// the
																						// presented
																						// algorithm
																						// owing
																						// to
																						// an
																						// optimization
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
								L_w_l_x_tprime.updateWithMax(L_w_l_x_tprime, H_t, C_t); // Body
																						// of
																						// loop
																						// at
																						// Line
																						// 8
																						// of
																						// algorithm.
																						// This
																						// is
																						// slightly
																						// different
																						// from
																						// the
																						// presented
																						// algorithm
																						// owing
																						// to
																						// an
																						// optimization
							}
						}
					}
				}
			}
		}

		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_l.updateWithMax(H_t, C_t); // Line 9 of algorithm

		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		P_l.copyFrom(P_t); // Line 9 of algorithm

		state.updateViewAsWriterAtRelease(getLock(), getThread()); // Line 10 of algorithm

		this.printRaceInfo(state, verbosity);

		state.incClockThread(getThread()); // Vector clock increment at the end of a
											// release event

		return false;
	}

	/************************************************/

	/**************** Read/Write **********************/
	@Override
	// Handler for Read event
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
		// Line 11 of algorithm
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
						P_t.updateWithMax(P_t, writeClock); // Body of loop at Line 11 of
															// algorithm
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

		// Check if an earlier write of same variable is incomparable with this read
		// event
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		// Update the Read(v) clock
		R_v.updateWithMax(R_v, C_t);

		return raceDetected;
	}

	// Handler for write event
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
		// Line 12 of algorithm
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
						P_t.updateWithMax(P_t, writeClock); // Body of loop at Line 12 of
															// algorithm
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
						P_t.updateWithMax(P_t, readClock); // Body of loop at Line 12 of
															// algorithm
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

		// Check if an earlier read of same variable is incomparable with this write
		// event
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		// Check if an earlier write of same variable is incomparable with this write
		// event
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		// Update the Write(v) clock
		W_v.updateWithMax(W_v, C_t);

		return raceDetected;
	}

	/************************************************/

	/***************** Fork/Join **********************/
	// Handler for Fork event
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
			H_tc.updateWithMax(C_t, H_t); // Update the HB predecessor of the child
											// (forked) thread

			VectorClock P_tc = state.getVectorClock(state.WCPPredecessorThread,
					this.getTarget());
			P_tc.updateWithMax(C_t, H_t); // Update the WCP predecessor of the child
											// (forked) thread

			this.printRaceInfo(state, verbosity);
			state.incClockThread(this.getThread());
		}
		return false;
	}

	// Handler for Join event
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

			H_t.updateWithMax(H_t, H_tc, C_tc); // Update the HB predecessor of this
												// thread
			P_t.updateWithMax(P_t, H_tc, C_tc); // Update the WCP predecessor of this
												// thread
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	/************************************************/

	@Override
	public void printRaceInfoTransactionType(WCPState state, int verbosity) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean HandleSubBegin(WCPState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(WCPState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
