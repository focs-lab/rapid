package engine.racedetectionengine.fhb;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class FHBEvent extends RaceDetectionEvent<FHBState> {

	@Override
	public boolean Handle(FHBState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(FHBState state, int verbosity) {
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
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(FHBState state, int verbosity) {
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
				str += "@" + state.getThreadIndex(this.getThread());
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(FHBState state, int verbosity) {
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
	public void printRaceInfoTransactionType(FHBState state, int verbosity) {
	}

	@Override
	public boolean HandleSubAcquire(FHBState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(FHBState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state, verbosity);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(FHBState state, int verbosity) {

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			state.addLocPair(state.getLWLocId(this.getVariable()), this.getLocId());
			// Force order
			C_t.updateWithMax(C_t, W_v);
		}

		this.printRaceInfo(state, verbosity);

		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		R_v.updateWithMax(R_v, C_t);

		state.setLastReadData(this.getVariable(), this.getThread(), this.getLocId(), C_t);

		state.incClockThread(getThread());

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(FHBState state, int verbosity) {

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (R_v.isLessThanOrEqual(W_v)) {
			if (!(W_v.isLessThanOrEqual(C_t))) {
				raceDetected = true;
				state.addLocPair(state.getLWLocId(this.getVariable()), this.getLocId());
			}
		} else {
			raceDetected = state.checkRaceWithReadsAndAddLocPairs(this.getThread(),
					this.getVariable(), C_t, this.getLocId());
		}

		C_t.updateWithMax(C_t, W_v, R_v);

		this.printRaceInfo(state, verbosity);

		W_v.copyFrom(C_t);

		state.setLWLocId(this.getVariable(), this.getLocId());

		state.incClockThread(getThread());

		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(FHBState state, int verbosity) {
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
	public boolean HandleSubJoin(FHBState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubBegin(FHBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(FHBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
