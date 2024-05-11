package engine.racedetectionengine.shb_epoch;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.FullAdaptiveVC;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class SHBEpochEvent extends RaceDetectionEvent<SHBEpochState> {

	@Override
	public boolean Handle(SHBEpochState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(SHBEpochState state, int verbosity) {
		if(this.getType().isLockType()){
			if(verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoAccessType(SHBEpochState state, int verbosity) {
		if(this.getType().isAccessType()){
			if(verbosity == 1 || verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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
	public void printRaceInfoExtremeType(SHBEpochState state, int verbosity) {
		if(this.getType().isExtremeType()){
			if(verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}
	
	@Override
	public void printRaceInfoTransactionType(SHBEpochState state, int verbosity) {
	}

	@Override
	public boolean HandleSubAcquire(SHBEpochState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SHBEpochState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state, verbosity);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SHBEpochState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		FullAdaptiveVC W_v  = state.getAdaptiveVC(state.writeVariable, getVariable());
		
		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		C_t.updateWithMax(C_t, LW_v);

		SemiAdaptiveVC R_v  = state.getAdaptiveVC(state.readVariable, getVariable());
		R_v.updateWithMax(C_t, state.getThreadIndex(this.getThread()));

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SHBEpochState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		FullAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());
		
		this.printRaceInfo(state, verbosity);
		
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		boolean W_v_isLTE_C_t = W_v.isLTEUpdateWithMax(C_t, state.getThreadIndex(this.getThread()));
		if(! W_v_isLTE_C_t){
			raceDetected = true;
		}
		
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		state.setLWLocId(this.getVariable(), this.getLocId());
		state.incClockThread(getThread());
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SHBEpochState state, int verbosity) {
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
	public boolean HandleSubJoin(SHBEpochState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}


	@Override
	public boolean HandleSubBegin(SHBEpochState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(SHBEpochState state, int verbosity) {
		return false;
	}

}
