package engine.atomicity.conflictserializability.atom_sanitizer;

import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;

public class AtomSanitizerEvent extends AtomicityEvent<AtomSanitizerState> {
	@Override
	public boolean Handle(AtomSanitizerState state) {
		if (AtomSanitizerState.VERBOSE) {
			System.out.println(this.toCompactString());
		}
		return super.Handle(state);
	}

	@Override
	public void printRaceInfoLockType(AtomSanitizerState state) {
		if (state.verbosity == 2 && this.getType().isLockType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getLock() + "|"
					+ this.getThread().getName());
		}
	}

	@Override
	public void printRaceInfoAccessType(AtomSanitizerState state) {
		if ((state.verbosity == 1 || state.verbosity == 2) && this.getType().isAccessType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getVariable().getName() + "|"
					+ this.getThread().getName() + "|" + this.getAuxId());
		}
	}

	@Override
	public void printRaceInfoExtremeType(AtomSanitizerState state) {
		if (state.verbosity == 2 && this.getType().isExtremeType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getTarget() + "|"
					+ this.getThread().getName());
		}
	}

	@Override
	public void printRaceInfoTransactionType(AtomSanitizerState state) {
		if (state.verbosity == 2 && this.getType().isTransactionType()) {
			System.out.println("#" + this.getLocId() + "|" + this.getType() + "|" + this.getThread().getName());
		}
	}

	@Override
	public boolean HandleSubAcquire(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Lock lock = this.getLock();
		int threadIndex = state.getThreadIndex(thread);
		int transactionId = state.getTransactionId(threadIndex);
		boolean outside = AtomSanitizerState.isBottomTransaction(transactionId);
		if (outside) {
			transactionId = state.getFreshTransactionId(threadIndex);
		}

		SimpleTransaction lastRelease = state.lockToLastReleaseTransaction.get(lock);
		if (lastRelease != null && lastRelease.threadIndex != threadIndex) {
			if (outside) {
				state.addPath(lastRelease.threadIndex, lastRelease.id, threadIndex, transactionId, true);
			} else {
				return state.checkCycleAndAddPath(lastRelease.threadIndex, lastRelease.id, threadIndex, transactionId);
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRelease(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		int threadIndex = state.getThreadIndex(thread);
		int transactionId = state.getTransactionId(threadIndex);
		if (AtomSanitizerState.isBottomTransaction(transactionId)) {
			transactionId = state.getFreshTransactionId(threadIndex);
		}

		state.setLastLockRelease(this.getLock(), threadIndex, transactionId);
		return false;
	}

	@Override
	public boolean HandleSubRead(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Variable variable = this.getVariable();
		int threadIndex = state.getThreadIndex(thread);
		int transactionId = state.getTransactionId(threadIndex);
		boolean outside = AtomSanitizerState.isBottomTransaction(transactionId);
		if (outside) {
			transactionId = state.getFreshTransactionId(threadIndex);
		}

		SimpleTransaction lastWrite = state.writeVariableToTransaction.get(variable);
		boolean violationDetected = false;
		if (lastWrite != null && lastWrite.threadIndex != threadIndex) {
			if (outside) {
				state.addPath(lastWrite.threadIndex, lastWrite.id, threadIndex, transactionId, true);
			} else {
				violationDetected = state.checkCycleAndAddPath(lastWrite.threadIndex, lastWrite.id, threadIndex,
						transactionId);
			}
		}

		state.setLastRead(variable, threadIndex, transactionId);
		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}

		Variable variable = this.getVariable();
		int threadIndex = state.getThreadIndex(thread);
		int transactionId = state.getTransactionId(threadIndex);
		boolean outside = AtomSanitizerState.isBottomTransaction(transactionId);
		if (outside) {
			transactionId = state.getFreshTransactionId(threadIndex);
		}

		boolean violationDetected = false;
		SimpleTransaction[] readers = state.readVariableThreadToTransaction.get(variable);
		if (readers != null) {
			for (SimpleTransaction reader : readers) {
				if (reader == null || reader.threadIndex == threadIndex) {
					continue;
				}
				if (outside) {
					state.addPath(reader.threadIndex, reader.id, threadIndex, transactionId, true);
				} else {
					violationDetected = violationDetected
							|| state.checkCycleAndAddPath(reader.threadIndex, reader.id, threadIndex, transactionId);
				}
			}
			state.clearLastReads(variable);
		}

		SimpleTransaction lastWrite = state.writeVariableToTransaction.get(variable);
		if (lastWrite != null && lastWrite.threadIndex != threadIndex) {
			if (outside) {
				state.addPath(lastWrite.threadIndex, lastWrite.id, threadIndex, transactionId, true);
			} else {
				violationDetected = violationDetected
						|| state.checkCycleAndAddPath(lastWrite.threadIndex, lastWrite.id, threadIndex, transactionId);
			}
		}

		state.setLastWrite(variable, threadIndex, transactionId);
		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(AtomSanitizerState state) {
		Thread parent = this.getThread();
		Thread child = this.getTarget();
		if (!state.isThreadRelevant(parent) || !state.isThreadRelevant(child)) {
			return false;
		}

		int parentIndex = state.getThreadIndex(parent);
		int parentTransactionId = state.getTransactionId(parentIndex);
		if (AtomSanitizerState.isBottomTransaction(parentTransactionId)) {
			parentTransactionId = state.getFreshTransactionId(parentIndex);
		}

		int childIndex = state.getThreadIndex(child);
		int childTransactionId = state.getFreshTransactionId(childIndex);
		state.addPath(parentIndex, parentTransactionId, childIndex, childTransactionId, true);
		return false;
	}

	@Override
	public boolean HandleSubJoin(AtomSanitizerState state) {
		Thread thread = this.getThread();
		Thread child = this.getTarget();
		if (!state.isThreadRelevant(thread) || !state.isThreadRelevant(child)) {
			return false;
		}

		int threadIndex = state.getThreadIndex(thread);
		int transactionId = state.getTransactionId(threadIndex);
		if (AtomSanitizerState.isBottomTransaction(transactionId)) {
			transactionId = state.getFreshTransactionId(threadIndex);
		}

		int childIndex = state.getThreadIndex(child);
		int childJoinTransactionId = state.getFreshTransactionId(childIndex);
		return state.checkCycleAndAddPath(childIndex, childJoinTransactionId, threadIndex, transactionId);
	}

	@Override
	public boolean HandleSubBegin(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}
		state.enterTransactionBookkeeping(thread);
		return false;
	}

	@Override
	public boolean HandleSubEnd(AtomSanitizerState state) {
		Thread thread = this.getThread();
		if (!state.isThreadRelevant(thread)) {
			return false;
		}
		state.exitTransactionBookkeeping(thread);
		return false;
	}
}
