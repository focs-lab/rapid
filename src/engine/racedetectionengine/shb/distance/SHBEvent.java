package engine.racedetectionengine.shb.distance;

import java.util.HashMap;

import event.Thread;
import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class SHBEvent extends RaceDetectionEvent<SHBState> {

	@Override
	public boolean Handle(SHBState state, int verbosity) {
		boolean toReturn = this.HandleSub(state, verbosity);

		// Racy vars
		if (toReturn) {
			state.racyVars.add(this.getVariable().getId());
		}

		return toReturn;
	}

	@Override
	public void printRaceInfoLockType(SHBState state, int verbosity) {
		if (this.getType().isLockType()) {
			if (verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
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
	public void printRaceInfoAccessType(SHBState state, int verbosity) {
		if (this.getType().isAccessType()) {
			if (verbosity == 1 || verbosity == 2) {
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
	public void printRaceInfoExtremeType(SHBState state, int verbosity) {
		if (this.getType().isExtremeType()) {
			if (verbosity == 2) {
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
	public boolean HandleSubAcquire(SHBState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SHBState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state, verbosity);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SHBState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		long d_min_across_threads_w = 0;
		long d_max_across_threads_w = 0;
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			HashMap<Thread, Long> confAuxIds = state
					.getVectorClock(state.writeVariableAuxId, getVariable());
			d_min_across_threads_w = this.getAuxId()
					- state.getMaxAuxId(W_v, C_t, confAuxIds, this.getThread());
			d_max_across_threads_w = this.getAuxId()
					- state.getMinAuxId(W_v, C_t, confAuxIds, this.getThread());
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

		C_t.updateWithMax(C_t, LW_v);

		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		int c_t_t = state.getIndex(C_t, this.getThread());
		state.setIndex(R_v, this.getThread(), c_t_t);

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
	public boolean HandleSubWrite(SHBState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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
					- state.getMaxAuxId(R_v, C_t, confAuxIds, this.getThread());
			d_max_across_threads_r = this.getAuxId()
					- state.getMinAuxId(R_v, C_t, confAuxIds, this.getThread());
		}
		long d_min_across_threads_w = 0;
		long d_max_across_threads_w = 0;
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			HashMap<Thread, Long> confAuxIds = state
					.getVectorClock(state.writeVariableAuxId, getVariable());
			d_min_across_threads_w = this.getAuxId()
					- state.getMaxAuxId(W_v, C_t, confAuxIds, this.getThread());
			d_max_across_threads_w = this.getAuxId()
					- state.getMinAuxId(W_v, C_t, confAuxIds, this.getThread());
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

		int c_t_t = state.getIndex(C_t, this.getThread());
		state.setIndex(W_v, this.getThread(), c_t_t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		state.setLWLocId(this.getVariable(), this.getLocId());
		state.incClockThread(getThread());

		HashMap<Thread, Long> lastAuxIds = state.getVectorClock(state.writeVariableAuxId,
				getVariable());
		lastAuxIds.put(this.getThread(), this.getAuxId());

		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SHBState state, int verbosity) {
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
	public boolean HandleSubJoin(SHBState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(SHBState state, int verbosity) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean HandleSubBegin(SHBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(SHBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
