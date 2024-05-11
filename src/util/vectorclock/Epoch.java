package util.vectorclock;

public class Epoch {

	private int clock;
	private int threadIdx;

	public Epoch() {
		this.clock = 0;
		this.threadIdx = 0;
	}
	
	public Epoch(int c, int t) {
		this.clock = c;
		this.threadIdx = t;
	}
	
	public Epoch(Epoch fromEpoch) {
		this.clock = fromEpoch.clock;
		this.threadIdx = fromEpoch.threadIdx;
	}

	public int getClock() {
		return this.clock;
	}
	
	public int getThreadIndex(){
		return this.threadIdx;
	}
	
	public void setClock(int c){
		this.clock = c;
	}
	
	public void setThreadIndex(int t){
		this.threadIdx = t;
	}
	
	public String toString() {
		return Integer.toString(this.clock) + "@" + Integer.toString(this.threadIdx) ;
	}

	public boolean isZero() {
		return this.clock == 0;
	}

	public boolean isEqual(Epoch epoch) {
		return (this.clock == epoch.clock) && (this.threadIdx == epoch.threadIdx);
	}

	public boolean isLessThanOrEqual(VectorClock vc) {
		return this.clock <= vc.getClock().get(this.threadIdx);
	}
}