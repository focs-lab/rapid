package engine.racedetectionengine;

import event.Event;

public abstract class RaceDetectionEvent<S extends State> extends Event {
	public RaceDetectionEvent(){
		super();
	}
	
	public void copyFrom(Event fromEvent){
		super.copyFrom(fromEvent);
	}
	
	public void printRaceInfo(S state, int verbosity){
		if(this.getType().isLockType())  this.printRaceInfoLockType(state, verbosity);
		if(this.getType().isAccessType()) this.printRaceInfoAccessType(state, verbosity);
		if(this.getType().isExtremeType()) this.printRaceInfoExtremeType(state, verbosity);
	}
	
	public abstract boolean Handle(S state, int verbosity);
	
	public boolean HandleSub(S state, int verbosity){
		boolean raceDetected = false;

		if(this.getType().isAcquire()) 	raceDetected = this.HandleSubAcquire(state, verbosity);
		if(this.getType().isRelease()) 	raceDetected = this.HandleSubRelease(state, verbosity);
		if(this.getType().isRead())		raceDetected = this.HandleSubRead(state, verbosity);
		if(this.getType().isWrite())	raceDetected = this.HandleSubWrite(state, verbosity);
		if(this.getType().isFork()) 	raceDetected = this.HandleSubFork(state, verbosity);
		if(this.getType().isJoin())		raceDetected = this.HandleSubJoin(state, verbosity);
		if(this.getType().isBegin()) 	raceDetected = this.HandleSubBegin(state, verbosity);
		if(this.getType().isEnd())		raceDetected = this.HandleSubEnd(state, verbosity);

		return raceDetected;
	}
	
	public abstract void printRaceInfoLockType(S state, int verbosity);
	public abstract void printRaceInfoAccessType(S state, int verbosity);
	public abstract void printRaceInfoExtremeType(S state, int verbosity);
	public abstract void printRaceInfoTransactionType(S state, int verbosity);
	
	public abstract boolean HandleSubAcquire(S state, int verbosity);
	public abstract boolean HandleSubRelease(S state, int verbosity);
	public abstract boolean HandleSubRead(S state, int verbosity);
	public abstract boolean HandleSubWrite(S state, int verbosity);
	public abstract boolean HandleSubFork(S state, int verbosity);
	public abstract boolean HandleSubJoin(S state, int verbosity);
	public abstract boolean HandleSubBegin(S state, int verbosity);
	public abstract boolean HandleSubEnd(S state, int verbosity);

}
