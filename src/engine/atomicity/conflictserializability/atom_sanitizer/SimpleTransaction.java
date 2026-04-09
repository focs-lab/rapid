package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.Objects;

final class SimpleTransaction {
	int threadIndex;
	int id;

	SimpleTransaction(int threadIndex, int transactionId) {
		this.threadIndex = threadIndex;
		this.id = transactionId;
	}

	void update(int threadIndex, int transactionId) {
		this.threadIndex = threadIndex;
		this.id = transactionId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SimpleTransaction other = (SimpleTransaction) obj;
		return threadIndex == other.threadIndex && id == other.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(threadIndex, id);
	}

	@Override
	public String toString() {
		return "<" + threadIndex + ":" + id + ">";
	}
}
