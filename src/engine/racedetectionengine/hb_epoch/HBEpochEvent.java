package engine.racedetectionengine.hb_epoch;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class HBEpochEvent extends RaceDetectionEvent<HBEpochState> {

	@Override
	public boolean Handle(HBEpochState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(HBEpochState state, int verbosity) {
		if(this.getType().isLockType()){
			if(verbosity == 2){
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
	}

	@Override
	public void printRaceInfoAccessType(HBEpochState state, int verbosity) {
		if(this.getType().isAccessType()){
			if(verbosity == 1 || verbosity == 2){
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
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}	
		}		
	}

	@Override
	public void printRaceInfoExtremeType(HBEpochState state, int verbosity) {
		if(this.getType().isExtremeType()){
			if(verbosity == 2){
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
	}
	
	@Override
	public void printRaceInfoTransactionType(HBEpochState state, int verbosity) {
	}

	@Override
	public boolean HandleSubAcquire(HBEpochState state, int verbosity) {
		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		H_t.updateWithMax(H_t, L_l);
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(HBEpochState state, int verbosity) {
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		state.incClockThread(getThread());
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRead(HBEpochState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		else{
			int tIndex = state.getThreadIndex(this.getThread());
			int c = C_t.getClockIndex(tIndex);
			if(!R_v.isSameEpoch(c, tIndex)){
				R_v.updateWithMax(C_t, state.getThreadIndex(this.getThread()));
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(HBEpochState state, int verbosity) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		int tIndex = state.getThreadIndex(this.getThread());
		int c = C_t.getClockIndex(tIndex);
		if(!W_v.isSameEpoch(c, tIndex)){
			W_v.setEpoch(c, tIndex);
			if(!R_v.isEpoch()){
				R_v.forceBottomEpoch();
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(HBEpochState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());			
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread, this.getTarget());
			// System.out.println("Fork : Setting HB of target");
			H_tc.copyFrom(C_t);
			state.incClockThread(this.getThread());
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(HBEpochState state, int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			VectorClock C_tc = state.generateVectorClockFromClockThread(this.getTarget());
			H_t.updateWithMax(H_t, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}


	@Override
	public boolean HandleSubBegin(HBEpochState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(HBEpochState state, int verbosity) {
		return false;
	}

}
