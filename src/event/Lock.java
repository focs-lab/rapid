package event;

public class Lock extends Decoration {
	
	public static int lockCountTracker = 0;

	public Lock() {
		this.id = lockCountTracker;
		lockCountTracker++;
		this.name = "__lock::" + Integer.toString(this.id) + "__";
	}

	public Lock(String sname) {
		this.id = lockCountTracker;
		lockCountTracker++;
		this.name = sname;
	}

}
