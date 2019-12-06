package event;

import java.util.HashSet;

public class Event {
	//Data for Event
	public static Long eventCountTracker = (long) 0;
	protected Long id;
	protected Long auxId; 	// RV event identifier
	protected int locId;  	// location Id given by RV
	protected String name;
	protected EventType type;
	protected Thread thread;
	
	//Data for Acquire/Release
	protected Lock lock;
	protected HashSet<Variable> readVarSet;
	protected HashSet<Variable> writeVarSet;
	
	//Data for Read/Write
	protected Variable variable;
	private HashSet<Lock> lockSet;
	
	//Data for Fork/Join
	protected Thread target;
	
	public void updateEventLockType(Lock l){
		if(this.getType().isLockType()){
			this.readVarSet = new HashSet<Variable>();
			this.writeVarSet = new HashSet<Variable>();
			if(l == null) throw new IllegalArgumentException("Lock is null : EventType - " + this.getType().toString());
			this.lock = l;
		}
	}
	
	public void updateEventAccessType(Variable var){
		if(this.getType().isAccessType()){
			lockSet = new HashSet<Lock>();
			if(var == null) throw new IllegalArgumentException("Var is null : EventType - " + this.getType().toString());
			this.variable = var;
		}
	}
	
	public void updateEventExtremeType(Thread tar){
		if(this.getType().isExtremeType()){
			if(tar == null) throw new IllegalArgumentException("Target is null : EventType - " + this.getType().toString());
			this.target = tar;
		}
	}
	
	public void updateEvent(Long aux_id, int loc_id, String sname, EventType tp, Thread th) {
		this.id = eventCountTracker;
		eventCountTracker++;
		this.auxId = aux_id;
		this.locId = loc_id;
		this.name = "__event::" + sname + "__";
		this.type = tp;
		this.thread = th;
	}

	public void updateEvent(Long aux_id, int loc_id, String sname, EventType tp, Thread th, Lock l, Variable var, Thread tar) {		
		this.updateEvent(aux_id, loc_id, sname, tp, th);
		
		//Acquire/Release
		this.updateEventLockType(l);
		
		//Read/Write
		this.updateEventAccessType(var);
		
		//Fork/Join
		this.updateEventExtremeType(tar);
	}
	
	public void updateEvent(){
		this.updateEvent(0L, 0, "dummy", EventType.DUMMY, null);
	}
	
	public Event() {
		this.updateEvent();
	}
	
	public Event(Long aux_id, int loc_id, EventType tp, Thread th, Lock l, Variable var, Thread tar) {
		this.updateEvent(aux_id, loc_id, Long.toString(eventCountTracker), tp, th, l, var, tar);
	}

	public Event(Long aux_id, int loc_id, String sname, EventType tp, Thread th, Lock l, Variable var, Thread tar) {
		this.updateEvent(aux_id, loc_id, sname, tp, th, l, var, tar);
	}

	public void copyFrom(Event fromEvent){
		this.id = fromEvent.getId();
		this.auxId = fromEvent.getAuxId();
		this.locId = fromEvent.getLocId();
		this.name = fromEvent.getName();
		this.type = fromEvent.getType();
		this.thread = fromEvent.getThread();
		
		//Data for Acquire/Release
		if(this.getType().isLockType()){
			this.lock = fromEvent.getLock();
			this.setReadVarSet(fromEvent.getReadVarSet());
			this.setWriteVarSet(fromEvent.getWriteVarSet());
		}
		
		//Data for Read/Write
		if(this.getType().isAccessType()){
			this.variable = fromEvent.getVariable();
			this.setLockSet(fromEvent.getLockSet());
		}
		
		//Data for Fork/Join
		if(this.getType().isExtremeType()){
			this.target = fromEvent.getTarget();
		}
	}
	
	public Long getId() {
		return this.id;
	}

	public Long getAuxId() {
		return this.auxId;
	}
	
	public int getLocId() {
		return this.locId;
	}

	public String getName() {
		return this.name;
	}

	public EventType getType() {
		return type;
	}
	
	public void setType(EventType tp) {
		this.type = tp;
	}

	public Thread getThread() {
		return thread;
	}
	
	public String toString() {
		return "(Event" + "-" + Long.toString(this.id) + "-AUX:" + Long.toString(this.auxId) + "-L" + Integer.toString(this.locId) + "-" + this.name + "-"
				+ this.type.toString() + " -" + this.thread.toString() + ")";
	}

	public String toFullStringForChildren() {
		return "(Event" + "-" + Long.toString(this.id) + "-AUX:" + Long.toString(this.auxId) + "-L" + Integer.toString(this.locId) + "-" + this.name + "-"
				+ this.type.toString() + " -" + this.thread.toString();
	}
	
	public String toFullString(){
		String str = "";
		
		if(this.getType().isLockType())		str = this.toFullStringLockType();
		if(this.getType().isAccessType())	str = this.toFullStringAccessType();
		if(this.getType().isExtremeType())	str = this.toFullStringExtremeType();
		
		return str;
	}
	
	public String toPrototypeString(){
		String str = "";
		
		if(this.getType().isLockType())		str = this.toPrototypeStringLockType();
		if(this.getType().isAccessType())	str = this.toPrototypeStringAccessType();
		if(this.getType().isExtremeType())	str = this.toPrototypeStringExtremeType();
		
		return str;
	}
	
	public String toCompactString(){
		String str = "";
		str = str + this.getThread().getName();
		str = str + ",";
		
		if(this.getType().isAccessType()){
			if(this.getType().isRead()){
				str = str + "R";
			}
			else if(this.getType().isWrite()){
				str = str + "W";
			}
			str = str + ",";
			str = str + this.getVariable().getName();
		}
		
		else if(this.getType().isLockType()){
			if(this.getType().isAcquire()){
				str = str + "L";
			}
			else if(this.getType().isRelease()){
				str = str + "U";
			}
			str = str + ",";
			str = str + this.getLock().getName();
		}
		
		else if(this.getType().isExtremeType()){
			if(this.getType().isFork()){
				str = str + "F";
			}
			else if(this.getType().isJoin()){
				str = str + "J";
			}
			str = str + ",";
			str = str + this.getTarget().getName();
		}
		
		return str;
	}
	
	/**************Acquire/Release*******************/
	public Lock getLock() {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation getLock() for EventType " + this.getType().toString());
		return this.lock;
	}
	
	public HashSet<Variable> getReadVarSet() {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation getReadVarSet() for EventType " + this.getType().toString());
		return readVarSet;
	}

	public HashSet<Variable> getWriteVarSet() {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation getWriteVarSet() for EventType " + this.getType().toString());
		return writeVarSet;
	}
	
	public void setReadVarSet(HashSet<Variable> rSet) {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation setReadVarSet() for EventType " + this.getType().toString());
		// You have to do a deep copy here
		this.readVarSet = new HashSet<Variable>(rSet);
	}
	
	public void setWriteVarSet(HashSet<Variable> wSet) {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation setWriteVarSet() for EventType " + this.getType().toString());
		// You have to do a deep copy here
		this.writeVarSet = new HashSet<Variable>(wSet);
	}
	
	public void addReadVariable(Variable v) {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation addReadVariable() for EventType " + this.getType().toString());
		readVarSet.add(v);
	}

	public void addWriteVariable(Variable v) {
		if (! this.getType().isLockType()) throw new IllegalArgumentException("Illegal operation addWriteVariable() for EventType " + this.getType().toString());
		writeVarSet.add(v);
	}
		
	public String toFullStringLockType() {
		return toFullStringForChildren() + "-" + this.getLock().toString() + "-" + "readVars="
				+ this.getReadVarSet().toString() + "-" + "writeVars=" + this.getWriteVarSet().toString() + ")";
	}
	
	public String toPrototypeStringLockType() {
		return "@  Acquire(T" + Integer.toString(this.getThread().getId()) + ",L"
				+ Integer.toString(this.getLock().getId()) + ")";
	}
	
	public String toStandardFormat(){
		String sensibleStr = this.getThread().getName();
		sensibleStr = sensibleStr + "|" + this.getType().toStandardFormat();
		if(this.getType().isAccessType()){
			sensibleStr = sensibleStr + "(" + this.getVariable().getName() + ")"; 
		}
		else if(this.getType().isLockType()){
			sensibleStr = sensibleStr + "(" + this.getLock().getName() + ")"; 
		}
		else if(this.getType().isExtremeType()){
			sensibleStr = sensibleStr + "(" + this.getTarget().getName() + ")"; 
		}
		sensibleStr = sensibleStr + "|" + this.getLocId();
		return sensibleStr;
	}
	/************************************************/
	
	/****************Read/Write**********************/
	public Variable getVariable() {
		if (! this.getType().isAccessType()) throw new IllegalArgumentException("Illegal operation getVariable() for EventType " + this.getType().toString());		
		return this.variable;
	}
	
	public HashSet<Lock> getLockSet() {
		if (! this.getType().isAccessType()) throw new IllegalArgumentException("Illegal operation getLockSet() for EventType " + this.getType().toString());
		return this.lockSet;
	}

	public void setLockSet(HashSet<Lock> lSet) {
		if (! this.getType().isAccessType()) throw new IllegalArgumentException("Illegal operation setLockSet() for EventType " + this.getType().toString());
		//Do a deep copy here
		this.lockSet = new HashSet<Lock>(lSet);
	}
	
	public String toFullStringAccessType() {
		return toFullStringForChildren() 
				+ "-" + this.getVariable().toString()
				+ "-" + "lockSet=" + this.getLockSet()
				+ ")";
	}
	
	public String toPrototypeStringAccessType() {
		return "@  Rd(T" + Integer.toString(this.getThread().getId()) + ",V"
				+ Integer.toString(this.getVariable().getId()) + ")";
	}
	/************************************************/
	
	/*****************Fork/Join**********************/
	public Thread getTarget() {
		if (! this.getType().isExtremeType()) throw new IllegalArgumentException("Illegal operation getTarget() for EventType " + this.getType().toString());
		return this.target;
	}
	
	public String toFullStringExtremeType() {
		return toFullStringForChildren() + "-" + this.getTarget().toString() + ")";
	}
	
	public String toPrototypeStringExtremeType() {
		return "@  Start(T" + Integer.toString(this.getThread().getId()) + ",T"
				+ Integer.toString(this.getTarget().getId()) + ")";
	}
	/************************************************/
}