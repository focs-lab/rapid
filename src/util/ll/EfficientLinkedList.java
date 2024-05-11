package util.ll;

public class EfficientLinkedList<T> {
	private int sz;
	private EfficientNode<T> headNode;
	private EfficientNode<T> tailNode;

	public EfficientLinkedList() {
		this.sz = 0;
		this.headNode = null;
		this.tailNode = null;
	}

	public int getLength() {
		return this.sz;
	}

	public boolean isEmpty() {
		return (sz <= 0);
	}

	// Earliest inserted element
	public T bottom() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get bottom: Store is empty");
		}
		return this.headNode.getData();
	}

	// Latest inserted element
	public T top() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get top: Store is empty");
		}
		return this.tailNode.getData();
	}

	public T removeBottom() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot remove first: Store is empty");
		}
		T retData = this.headNode.getData();
		if (sz == 1) {
			this.headNode = null;
			this.tailNode = null;
		} else {
			EfficientNode<T> nextNode = this.headNode.getNext();
			this.headNode = nextNode;
		}
		this.sz = this.sz - 1;
		return retData;
	}

	public void removeBottomPrefixOfLength(int i) {
		if (i > this.sz) {
			throw new IllegalArgumentException("Array out of bound: removePrefix : i ="
					+ Integer.toString(i) + ", size = " + Integer.toString(this.sz));
		}
		for (int k = 0; k < i; k++) {
			this.removeBottom();
		}
	}

	public void pushTop(T data) {
		EfficientNode<T> newNode = new EfficientNode<T>(data);
		if (this.isEmpty()) {
			this.headNode = newNode;
			this.tailNode = newNode;
		} else {
			this.tailNode.setNext(newNode);
			this.tailNode = newNode;
		}
		this.sz = this.sz + 1;
	}

	public void setTop(T data) {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get top: Store is empty");
		}
		this.tailNode.setData(data);
	}

	public EfficientNode<T> getHeadNode() {
		return this.headNode;
	}

	public String toString() {
		String strPre = "[";
		String strPost = "]";
		String strMid = "";
		if (sz >= 1) {
			strMid = strMid + this.headNode.getData().toString();
			EfficientNode<T> itrNode = this.headNode.getNext();
			for (int i = 1; i < sz; i++) {
				strMid = strMid + ", " + itrNode.getData().toString();
				itrNode = itrNode.getNext();
			}
		}
		return strPre + strMid + strPost;
	}
}
