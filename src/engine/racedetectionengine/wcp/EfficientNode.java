package engine.racedetectionengine.wcp;

//The node of the queue data structure
public class EfficientNode {
	private WCPClockPair clockPair;
	private EfficientNode next;
	
	public EfficientNode() {
		this.clockPair = null;
		this.next = null;
	}
	
	public EfficientNode(WCPClockPair cp){
		this.clockPair = cp;
		this.next = null;
	}
	
	public WCPClockPair getData(){
		return this.clockPair;
	}
	
	public EfficientNode getNext(){
		return this.next;
	}
	
	public void setNext(EfficientNode n){
		this.next = n;
	}
	
	public boolean hasNext(){
		return !(this.next == null);
	}
}	
