package engine.atomicity.conflictserializability.atom_sanitizer;

import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;

public class AtomSanitizerIncEvent extends AtomicityEvent<AtomSanitizerIncState> {
	@Override
	public boolean Handle(AtomSanitizerIncState state) {
		if (AtomSanitizerIncState.VERBOSE) {
			System.out.println(this.toCompactString());
		}

		boolean violationDetected = super.Handle(state);
		if (violationDetected) {
			state.setBlame(this.getThread());
		}
		return violationDetected;
	}

	@Override
	public void printRaceInfoLockType(AtomSanitizerIncState state) {
		if (state.verbosity == 2 && this.getType().isLockType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getLock() + "|"
					+ this.getThread().getName());
		}
	}

	@Override
	public void printRaceInfoAccessType(AtomSanitizerIncState state) {
		if ((state.verbosity == 1 || state.verbosity == 2) && this.getType().isAccessType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getVariable().getName() + "|"
					+ this.getThread().getName() + "|" + this.getAuxId());
		}
	}

	@Override
	public void printRaceInfoExtremeType(AtomSanitizerIncState state) {
		if (state.verbosity == 2 && this.getType().isExtremeType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getTarget() + "|"
					+ this.getThread().getName());
		}
	}

	@Override
	public void printRaceInfoTransactionType(AtomSanitizerIncState state) {
		if (state.verbosity == 2 && this.getType().isTransactionType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getThread().getName());
		}
	}

	@Override
	public boolean HandleSubAcquire(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Lock lock = this.getLock();
		int threadIndex = state.getThreadIndex(thread);
		int currentEventId = state.getAndIncrementEventId(threadIndex);
		int currentTransactionId = state.getTransactionId(threadIndex);
		boolean outside = state.isBottomTransaction(currentTransactionId);
		if (outside) {
			currentTransactionId = state.getFreshTransactionId(threadIndex);
		}

		TransactionEvent lastRelease = state.getLastLockRelease(lock);
		boolean violationDetected = false;
		if (lastRelease != null) {
			if (!outside) {
				violationDetected = state.checkViolation(lastRelease, threadIndex, currentTransactionId);
			}
			state.addPath(lastRelease, threadIndex, currentEventId);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		int threadIndex = state.getThreadIndex(thread);
		int currentEventId = state.getAndIncrementEventId(threadIndex);
		int currentTransactionId = state.getTransactionId(threadIndex);
		if (state.isBottomTransaction(currentTransactionId)) {
			currentTransactionId = state.getFreshTransactionId(threadIndex);
		}

		state.setLastLockRelease(this.getLock(), threadIndex, currentTransactionId, currentEventId);
		return false;
	}

	@Override
	public boolean HandleSubRead(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Variable variable = this.getVariable();
		int threadIndex = state.getThreadIndex(thread);
		int currentEventId = state.getAndIncrementEventId(threadIndex);
		int currentTransactionId = state.getTransactionId(threadIndex);
		boolean outside = state.isBottomTransaction(currentTransactionId);
		if (outside) {
			currentTransactionId = state.getFreshTransactionId(threadIndex);
		}

		boolean violationDetected = false;
		TransactionEvent lastWrite = state.getLastWrite(variable);
		if (lastWrite != null) {
			if (!outside) {
				violationDetected = state.checkViolation(lastWrite, threadIndex, currentTransactionId);
			}
			state.addPath(lastWrite, threadIndex, currentEventId);
		}

		state.setLastRead(variable, threadIndex, currentTransactionId, currentEventId);
		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Variable variable = this.getVariable();
		int threadIndex = state.getThreadIndex(thread);
		int currentEventId = state.getAndIncrementEventId(threadIndex);
		int currentTransactionId = state.getTransactionId(threadIndex);
		boolean outside = state.isBottomTransaction(currentTransactionId);
		if (outside) {
			currentTransactionId = state.getFreshTransactionId(threadIndex);
		}

		boolean violationDetected = false;
		TransactionEvent[] lastReads = state.getLastReads(variable);
		if (lastReads != null) {
			for (TransactionEvent lastRead : lastReads) {
				if (lastRead == null) {
					continue;
				}
				if (!outside) {
					violationDetected = violationDetected || state.checkViolation(lastRead, threadIndex,
							currentTransactionId);
				}
				state.addPath(lastRead, threadIndex, currentEventId);
			}
		}

		TransactionEvent lastWrite = state.getLastWrite(variable);
		if (lastWrite != null) {
			if (!outside) {
				violationDetected = violationDetected || state.checkViolation(lastWrite, threadIndex,
						currentTransactionId);
			}
			state.addPath(lastWrite, threadIndex, currentEventId);
		}

		state.setLastWrite(variable, threadIndex, currentTransactionId, currentEventId);
		state.clearLastReads(variable);
		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(AtomSanitizerIncState state) {
		Thread parent = this.getThread();
		Thread child = this.getTarget();
		if (!state.isThreadRelevant(parent) || !state.isThreadRelevant(child)) {
			return false;
		}

		int parentIndex = state.getThreadIndex(parent);
		int currentEventId = state.getAndIncrementEventId(parentIndex);
		int currentTransactionId = state.getTransactionId(parentIndex);
		if (state.isBottomTransaction(currentTransactionId)) {
			currentTransactionId = state.getFreshTransactionId(parentIndex);
		}
		TransactionEvent currentTransaction = new TransactionEvent(parentIndex, currentTransactionId, currentEventId);

		int childIndex = state.getThreadIndex(child);
		int childEventId = state.getAndIncrementEventId(childIndex);
		state.addPath(currentTransaction, childIndex, childEventId);
		return false;
	}

	@Override
	public boolean HandleSubJoin(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		Thread child = this.getTarget();
		if (!state.isThreadRelevant(thread) || !state.isThreadRelevant(child)) {
			return false;
		}

		int threadIndex = state.getThreadIndex(thread);
		int currentTransactionId = state.getTransactionId(threadIndex);
		int currentEventId = state.getAndIncrementEventId(threadIndex);
		boolean outside = state.isBottomTransaction(currentTransactionId);
		if (outside) {
			currentTransactionId = state.getFreshTransactionId(threadIndex);
		}

		int childIndex = state.getThreadIndex(child);
		int childEventId = state.getAndIncrementEventId(childIndex);
		int childTransactionId = state.getTransactionId(childIndex);
		if (state.isBottomTransaction(childTransactionId)) {
			childTransactionId = state.getFreshTransactionId(childIndex);
		}

		TransactionEvent childTransaction = new TransactionEvent(childIndex, childTransactionId, childEventId);
		boolean violationDetected = false;
		if (!outside) {
			violationDetected = state.checkViolation(childTransaction, threadIndex, currentTransactionId);
		}
		state.addPath(childTransaction, threadIndex, currentEventId);
		return violationDetected;
	}

	@Override
	public boolean HandleSubBegin(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}
		state.enterTransactionBookkeeping(thread);
		return false;
	}

	@Override
	public boolean HandleSubEnd(AtomSanitizerIncState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}
		state.exitTransactionBookkeeping(thread);
		return false;
	}
}
