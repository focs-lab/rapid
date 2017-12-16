package parse.util;

import event.EventType;

public class EventInfo {
	public EventType type;
	public String thread;
	public String decor;
	public String locId;
	
	public EventInfo(){}

	public EventInfo(EventType tp, String th, String dec, String l) {
		this.type = tp;
		this.thread = th;
		this.decor = dec;
		this.locId = l;
	}
	
	public void updateEventInfo(EventType tp, String th, String dec, String l){
		this.type = tp;
		this.thread = th;
		this.decor = dec;
		this.locId = l;
	}

	public String toString(){
		if (this.locId == ""){
			return this.thread + "|" + this.type.toString() + "|" +  this.decor ;
		}
		else{
			return this.locId + "|" +  this.thread + "|" + this.type.toString() + "|" +  this.decor ;
		}
	}
}
