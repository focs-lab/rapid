package engine.racedetectionengine.wcp;

//Data structure implementing the queue of the algorithm
public class EfficientStore {
	private int dim;
	private int sz;
	private EfficientNode headNode;
	private EfficientNode tailNode;

	public EfficientStore(int n) {
		this.dim = n;
		this.sz = 0;
		this.headNode = null;
		this.tailNode = null;
	}

	public int getLength() {
		return this.sz;
	}
	
	public boolean isEmpty(){
		return (sz <= 0);
	}

	public WCPClockPair bottom() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get bottom: Store is empty");
		}
		return this.headNode.getData();
	}

	public WCPClockPair top() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get top: Store is empty");
		}
		return this.tailNode.getData();
	}

	public WCPClockPair removeBottom() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot remove first: Store is empty");
		}
		//return this.store.removeFirst();
		WCPClockPair retPair = this.headNode.getData();
		if(sz == 1){
			this.headNode = null;
			this.tailNode = null;
		}
		else{
			EfficientNode nextNode = this.headNode.getNext();
			this.headNode = nextNode;
		}
		this.sz = this.sz - 1;
		return retPair;
	}

	public void removeBottomPrefixOfLength(int i) {
		if (i > this.sz) {
			throw new IllegalArgumentException("Array out of bound: removePrefix : i =" + Integer.toString(i)
					+ ", size = " + Integer.toString(this.sz));
		}
		for (int k = 0; k < i; k++) {
			this.removeBottom();
		}
	}
	

	public void pushTop(WCPClockPair clockPair) {
		if (clockPair.getDim() != this.dim) {
			throw new IllegalArgumentException("Dimension mismatch b/w store and clockpair");
		}
		EfficientNode newNode = new EfficientNode(clockPair);
		if(this.isEmpty()){
			this.headNode = newNode;
			this.tailNode = newNode;
		}
		else{
			this.tailNode.setNext(newNode);
			this.tailNode = newNode;
		}
		this.sz = this.sz + 1;
		//System.out.println("Pushed something. Stack looks like " + this);
	}
	
	public EfficientNode getheadNode(){
		return this.headNode;
	}
	
	public String toString(){
		String strPre = "[";
		String strPost = "]";
		String strMid = "";
		if(sz >= 1){
			strMid = strMid + this.headNode.getData().toString();
			EfficientNode itrNode = this.headNode.getNext();
			for(int i = 1; i < sz; i ++){
				strMid = strMid + ", " + itrNode.getData().toString();
				itrNode = itrNode.getNext();
			}
		}
		return strPre + strMid + strPost;
	}
}

