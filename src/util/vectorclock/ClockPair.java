package util.vectorclock;

public class ClockPair {

	private int dim;
	private VectorClock acquireClock;
	private VectorClock releaseClock;

	ClockPair(int dim) {
		this.dim = dim;
		this.acquireClock = new VectorClock(dim);
		this.releaseClock = new VectorClock(dim);
	}

	public ClockPair(VectorClock acquire) {
		this.dim = acquire.getDim();
		this.acquireClock = new VectorClock(acquire);
		this.releaseClock = new VectorClock(dim);
	}
	
	ClockPair(VectorClock acquire, VectorClock release) {
		if (acquire.getDim() != release.getDim()) {
			throw new IllegalArgumentException("Dimensions of acquire and release clocks do not match");
		}
		this.dim = acquire.getDim();
		this.acquireClock = new VectorClock(acquire);
		this.releaseClock = new VectorClock(release);
	}

	public int getDim() {
		return this.dim;
	}

	public VectorClock getAcquire() {
		return this.acquireClock;
	}

	public VectorClock getRelease() {
		return this.releaseClock;
	}

	public void setAcquire(VectorClock acquire) {
		if (this.dim != acquire.getDim()) {
			throw new IllegalArgumentException(
					"Dimension of argument acquire does not match with initialized dimension");
		}
		this.acquireClock.copyFrom(acquire);
	}

	public void setRelease(VectorClock release) {
		if (this.dim != release.getDim()) {
			throw new IllegalArgumentException(
					"Dimension of argument release does not match with initialized dimension");
		}
		this.releaseClock.copyFrom(release);
	}

	public String toString(){
		String str = "(" + this.acquireClock.toString() + " , " + this.releaseClock.toString() + ")";
		return str;
	}
}