package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.Objects;

final class TransactionEvent {
	int threadIndex;
	int transactionId;
	int eventId;
	boolean isUnary;

	TransactionEvent(int threadIndex, int transactionId, int eventId, boolean isUnary) {
		this.threadIndex = threadIndex;
		this.transactionId = transactionId;
		this.eventId = eventId;
		this.isUnary = isUnary;
	}

	TransactionEvent(int threadIndex, int transactionId, int eventId) {
		this(threadIndex, transactionId, eventId, false);
	}

	void update(int threadIndex, int transactionId, int eventId) {
		this.threadIndex = threadIndex;
		this.transactionId = transactionId;
		this.eventId = eventId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TransactionEvent other = (TransactionEvent) obj;
		return threadIndex == other.threadIndex && transactionId == other.transactionId
				&& eventId == other.eventId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(threadIndex, transactionId, eventId);
	}

	@Override
	public String toString() {
		return "<" + threadIndex + ":" + transactionId + ":" + eventId + ">";
	}
}
