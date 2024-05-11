package engine.racedetectionengine.hb;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class HBEvent extends RaceDetectionEvent<HBState> {
	public HBEvent() {
		super();
	}

	public boolean Handle(HBState state, int verbosity){
		return this.HandleSub(state, verbosity);
	}
	
	/**************Pretty Printing*******************/
	@Override
	public void printRaceInfoLockType(HBState state, int verbosity){
		if(verbosity >= 2){
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getLock().toString();
			str += "|";
			VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			System.out.println(str);
		}
	}

	@Override
	public void printRaceInfoAccessType(HBState state, int verbosity){
		if(verbosity >= 2){
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getVariable().getName();
			str += "|";
			VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			System.out.println(str);	
		}
	}
	
	@Override
	public void printRaceInfoExtremeType(HBState state, int verbosity){
		if(verbosity >= 2){
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getTarget().toString();
			str += "|";
			VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			System.out.println(str);
		}
	}
	/************************************************/

	/**************Acquire/Release*******************/
	@Override
	public boolean HandleSubAcquire(HBState state, int verbosity){
		VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(HBState state, int verbosity) {
		VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 			
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		state.incClockThread(getThread());
		this.printRaceInfo(state, verbosity);
		return false;
	}
	/************************************************/

	/****************Read/Write**********************/
	@Override
	public boolean HandleSubRead(HBState state, int verbosity) {

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		R_v.updateWithMax(R_v, C_t);		

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(HBState state, int verbosity) {

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		W_v.updateWithMax(W_v, C_t);

		return raceDetected;
	}
	/************************************************/

	/*****************Fork/Join**********************/
	@Override
	public boolean HandleSubFork(HBState state,int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 			
			VectorClock C_tc = state.getVectorClock(state.HBThread, this.getTarget()); 
			C_tc.updateWithMax(C_tc, C_t);
			state.incClockThread(this.getThread());
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(HBState state,int verbosity) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.HBThread, this.getThread()); 
			VectorClock C_tc = state.getVectorClock(state.HBThread, this.getTarget()); 
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state, verbosity);
		}
		return false;
	}
	/************************************************/

	@Override
	public void printRaceInfoTransactionType(HBState state, int verbosity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean HandleSubBegin(HBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(HBState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}
}
