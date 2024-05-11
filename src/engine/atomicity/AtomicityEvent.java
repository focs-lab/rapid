package engine.atomicity;

import event.Event;

public abstract class AtomicityEvent<S extends State> extends Event {
	public AtomicityEvent(){
		super();
	}
	
	public void copyFrom(Event fromEvent){
		super.copyFrom(fromEvent);
	}
	
	public void printRaceInfo(S state){
		if(this.getType().isLockType())  this.printRaceInfoLockType(state);
		if(this.getType().isAccessType()) this.printRaceInfoAccessType(state);
		if(this.getType().isExtremeType()) this.printRaceInfoExtremeType(state);
		if(this.getType().isTransactionType()) this.printRaceInfoTransactionType(state);
	}
	
	public boolean Handle(S state) {
		return this.HandleSub(state);
	}
	
	public boolean HandleSub(S state){
		boolean violationDetected = false;

		if(this.getType().isAcquire()) 	violationDetected = this.HandleSubAcquire(state);
		if(this.getType().isRelease()) 	violationDetected = this.HandleSubRelease(state);
		if(this.getType().isRead())		violationDetected = this.HandleSubRead(state);
		if(this.getType().isWrite())	violationDetected = this.HandleSubWrite(state);
		if(this.getType().isFork()) 	violationDetected = this.HandleSubFork(state);
		if(this.getType().isJoin())		violationDetected = this.HandleSubJoin(state);
		if(this.getType().isBegin())	violationDetected = this.HandleSubBegin(state);
		if(this.getType().isEnd())		violationDetected = this.HandleSubEnd(state);

		return violationDetected;
	}
	
	public abstract void printRaceInfoLockType(S state);
	public abstract void printRaceInfoAccessType(S state);
	public abstract void printRaceInfoExtremeType(S state);
	public abstract void printRaceInfoTransactionType(S state);
	
	public abstract boolean HandleSubAcquire(S state);
	public abstract boolean HandleSubRelease(S state);
	public abstract boolean HandleSubRead(S state);
	public abstract boolean HandleSubWrite(S state);
	public abstract boolean HandleSubFork(S state);
	public abstract boolean HandleSubJoin(S state);
	public abstract boolean HandleSubBegin(S state);
	public abstract boolean HandleSubEnd(S state);

}
