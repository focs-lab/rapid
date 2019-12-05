package engine.atomicity;

public abstract class State {
	
	//parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public abstract void printMemory();
}
