package util.ll;

public class EfficientNode<T> {
	private T data;
	private EfficientNode<T> next;

	public EfficientNode() {
		this.data = null;
		this.next = null;
	}

	public EfficientNode(T data) {
		this.data = data;
		this.next = null;
	}

	public T getData() {
		return this.data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public EfficientNode<T> getNext() {
		return this.next;
	}

	public void setNext(EfficientNode<T> n) {
		this.next = n;
	}

	public boolean hasNext() {
		return !(this.next == null);
	}
}
