package util.vectorclock;

public class ClockEpochPair {

	private int dim;
	private Epoch acquireClock;
	private VectorClock releaseClock;

	ClockEpochPair(int dim) {
		this.dim = dim;
		this.acquireClock = new Epoch();
		this.releaseClock = new VectorClock(dim);
	}

	public ClockEpochPair(Epoch acquire) {
		this.acquireClock = new Epoch(acquire);
	}
	
	public ClockEpochPair(Epoch acquire, VectorClock release) {
		this.dim = release.getDim();
		this.acquireClock = new Epoch(acquire);
		this.releaseClock = new VectorClock(release);
	}

	public int getDim() {
		return this.dim;
	}

	public Epoch getAcquire() {
		return this.acquireClock;
	}

	public VectorClock getRelease() {
		return this.releaseClock;
	}

	public void setAcquire(Epoch acquire) {
		this.acquireClock.copyFrom(acquire);
	}

	public void setRelease(VectorClock release) {
		this.releaseClock = new VectorClock(release);
	}

	public String toString(){
		String str = "(" + this.acquireClock.toString() + " , ";
		str = str + ((this.releaseClock != null) ? this.releaseClock.toString() : "null");
		str = str + ")";
		return str;
	}
}