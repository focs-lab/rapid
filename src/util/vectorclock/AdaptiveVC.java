package util.vectorclock;

public abstract class AdaptiveVC {

	protected boolean is_epoch;
	protected Epoch epoch;
	protected VectorClock vc;

	public AdaptiveVC() {
		this.is_epoch = true;
		this.epoch = new Epoch();
		this.vc = null;
	}

//	public AdaptiveVC(int dim) {
//		this.is_epoch = true;
//		this.epoch = new Epoch();
//		this.vc = new VectorClock(dim);
//	}

	public Epoch getEpoch() {
		return this.epoch;
	}

	public VectorClock getVC(){
		return this.vc;
	}

	public boolean isEpoch(){
		return is_epoch;
	}
	
	public String toString() {
		if(is_epoch){
			return this.epoch.toString();
		}
		else{
			return this.vc.toString();
		}
	}

	public boolean isLessThanOrEqual(VectorClock vc) {
		if(is_epoch){
			return this.epoch.getClock() <= vc.getClock().get(this.epoch.getThreadIndex());
		}
		else{
			return this.vc.isLessThanOrEqual(vc);
		}
	}
	
	//Returns true iff this.is_epoch and this.epoch.clock == vc[this.epoch.thread]
	public boolean isSameEpoch(int c, int t){
		if(is_epoch){
			return this.epoch.getClock() == c && this.epoch.getThreadIndex() == t;
		}
		else{
			return false;
		}
	}
	
	public int getClockIndex(int tIndex){
		if(this.is_epoch){
			if(this.epoch.getThreadIndex() == tIndex){
				return this.epoch.getClock();
			}
			else return 0;
		}
		else  return this.getVC().getClock().get(tIndex);
	}

	//Checks if this <= vc. If true, return true; else {this := this \join vc; return false}
	public abstract boolean isLTEUpdateWithMax(VectorClock vc, int t);
	
	public void setEpoch(int c, int t){
		if(!this.is_epoch){
			throw new IllegalArgumentException("setEpoch can only be invoked when the clock is an epoch");
		}
		this.epoch.setClock(c);
		this.epoch.setThreadIndex(t);
	}
	
	public void setClockIndex(int tIndex, int tValue){
		if(this.is_epoch){
			throw new IllegalArgumentException("setClockIndex can only be invoked when the clock is a VC, not an epoch");
		}
		this.vc.setClockIndex(tIndex, tValue);
	}
	
	public void forceBottomEpoch(){
		this.is_epoch = true;
		this.epoch.setClock(0);
		this.epoch.setThreadIndex(0);
	}
	
}