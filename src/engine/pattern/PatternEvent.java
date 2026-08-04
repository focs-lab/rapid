package engine.pattern;

import event.Event;

public abstract class PatternEvent<S extends State> extends Event {

    public String toHashString() {
        String basicInfo = type + "-" + this.getThread();
        if(type.isAccessType()) {
            return basicInfo + "-" + this.getVariable();
        }
        else if(type.isTransactionType()) {
            return basicInfo + "-";
        }
        else if(type.isLockType()) {
            return basicInfo + "-" + this.getLock();
        }
        else if(type.isExtremeType()) {
            return basicInfo + "-" + this.getTarget();
        }
        else {
            throw new IllegalArgumentException("Illegal type");
        }
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
	
	public abstract boolean HandleSubAcquire(S state);
	public abstract boolean HandleSubRelease(S state);
	public abstract boolean HandleSubRead(S state);
	public abstract boolean HandleSubWrite(S state);
	public abstract boolean HandleSubFork(S state);
	public abstract boolean HandleSubJoin(S state);
	public abstract boolean HandleSubBegin(S state);
	public abstract boolean HandleSubEnd(S state);
}
