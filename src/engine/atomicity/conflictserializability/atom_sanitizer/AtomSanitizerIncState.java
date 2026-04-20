package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;

public class AtomSanitizerIncState extends State {
	private static final int BOTTOM_TRANSACTION_ID = -1;
	protected static final boolean VERBOSE = false;

	public final HashMap<Thread, Integer> threadToIndex;
	private final int numThreads;
	private final int[] threadToCurrentTransaction;
	private final int[] threadToNestingDepth;
	private final int[] threadToLocalEventId;

	private final HashMap<Lock, TransactionEvent> lockToLastEvent;
	private final HashMap<Variable, TransactionEvent> lastWriteVariable;
	private final HashMap<Variable, TransactionEvent[]> lastReadVariable;

	private final IncrementalSimpleMaxCSSTs cssts;
	private TransactionEvent blame;

	public AtomSanitizerIncState(HashSet<Thread> threadSet, int verbosity) {
		System.out.println("Using AtomSanitizerInc");
		this.verbosity = verbosity;
		this.threadToIndex = new HashMap<Thread, Integer>();
		int threadIndex = 0;
		Iterator<Thread> iterator = threadSet.iterator();
		while (iterator.hasNext()) {
			this.threadToIndex.put(iterator.next(), threadIndex);
			threadIndex++;
		}

		this.numThreads = threadIndex;
		this.threadToCurrentTransaction = new int[numThreads];
		this.threadToNestingDepth = new int[numThreads];
		this.threadToLocalEventId = new int[numThreads];
		for (int i = 0; i < numThreads; i++) {
			this.threadToCurrentTransaction[i] = BOTTOM_TRANSACTION_ID;
			this.threadToNestingDepth[i] = 0;
			this.threadToLocalEventId[i] = 0;
		}

		this.lockToLastEvent = new HashMap<Lock, TransactionEvent>();
		this.lastReadVariable = new HashMap<Variable, TransactionEvent[]>();
		this.lastWriteVariable = new HashMap<Variable, TransactionEvent>();
		this.cssts = new IncrementalSimpleMaxCSSTs(numThreads);
		this.blame = null;
	}

	public boolean isThreadRelevant(Thread thread) {
		return this.threadToIndex.containsKey(thread);
	}

	public int getThreadIndex(Thread thread) {
		Integer threadIndex = this.threadToIndex.get(thread);
		if (threadIndex == null) {
			throw new IllegalArgumentException("Unknown thread " + thread);
		}
		return threadIndex.intValue();
	}

	public boolean isBottomTransaction(int transactionId) {
		return transactionId == BOTTOM_TRANSACTION_ID;
	}

	public int getAndIncrementEventId(int threadIndex) {
		int localEventId = this.threadToLocalEventId[threadIndex];
		this.threadToLocalEventId[threadIndex]++;
		return localEventId;
	}

	public int getFreshTransactionId(int threadIndex) {
		return this.getAndIncrementEventId(threadIndex);
	}

	public int getEventId(int threadIndex) {
		return this.threadToLocalEventId[threadIndex];
	}

	public int getTransactionId(int threadIndex) {
		return this.threadToCurrentTransaction[threadIndex];
	}

	public boolean checkViolation(TransactionEvent from, int toThreadIndex, int toTransactionId) {
		if (from.threadIndex == toThreadIndex) {
			return false;
		}
		return this.cssts.reachable(toThreadIndex, toTransactionId, from.threadIndex, from.eventId);
	}

	public void addPath(TransactionEvent from, int toThreadIndex, int toEventId) {
		if (from.threadIndex == toThreadIndex) {
			return;
		}

		this.cssts.insertEdgeWithOnlyBackwardClosureSpecial(from.threadIndex, from.transactionId, from.eventId,
				toThreadIndex, toEventId);

		if (VERBOSE) {
			System.out.println("addPath-complete");
			this.cssts.print();
		}
	}

	public void setLastLockRelease(Lock lock, int threadIndex, int transactionId, int eventId) {
		TransactionEvent previous = this.lockToLastEvent.get(lock);
		if (previous == null) {
			this.lockToLastEvent.put(lock, new TransactionEvent(threadIndex, transactionId, eventId));
		} else {
			previous.update(threadIndex, transactionId, eventId);
		}
	}

	public TransactionEvent getLastLockRelease(Lock lock) {
		return this.lockToLastEvent.get(lock);
	}

	public void setLastWrite(Variable variable, int threadIndex, int transactionId, int eventId) {
		TransactionEvent previous = this.lastWriteVariable.get(variable);
		if (previous == null) {
			this.lastWriteVariable.put(variable, new TransactionEvent(threadIndex, transactionId, eventId));
		} else {
			previous.update(threadIndex, transactionId, eventId);
		}
	}

	public TransactionEvent getLastWrite(Variable variable) {
		return this.lastWriteVariable.get(variable);
	}

	public void setLastRead(Variable variable, int threadIndex, int transactionId, int eventId) {
		TransactionEvent[] readList = this.lastReadVariable.get(variable);
		if (readList == null) {
			readList = new TransactionEvent[this.numThreads];
			this.lastReadVariable.put(variable, readList);
		}

		TransactionEvent previous = readList[threadIndex];
		if (previous == null) {
			readList[threadIndex] = new TransactionEvent(threadIndex, transactionId, eventId);
		} else {
			previous.update(threadIndex, transactionId, eventId);
		}
	}

	public TransactionEvent[] getLastReads(Variable variable) {
		if (!this.lastReadVariable.containsKey(variable)) {
			return null;
		}
		return this.lastReadVariable.get(variable);
	}

	public void clearLastReads(Variable variable) {
		if (this.lastReadVariable.containsKey(variable)) {
			this.lastReadVariable.put(variable, null);
		}
	}

	public void enterTransactionBookkeeping(Thread thread) {
		int threadIndex = getThreadIndex(thread);
		int currentDepth = this.threadToNestingDepth[threadIndex];
		this.threadToNestingDepth[threadIndex]++;
		if (currentDepth == 0) {
			this.threadToCurrentTransaction[threadIndex] = this.getFreshTransactionId(threadIndex);
		}
	}

	public void exitTransactionBookkeeping(Thread thread) {
		int threadIndex = getThreadIndex(thread);
		int currentDepth = this.threadToNestingDepth[threadIndex];
		this.threadToNestingDepth[threadIndex]--;
		if (currentDepth == 1) {
			this.threadToCurrentTransaction[threadIndex] = BOTTOM_TRANSACTION_ID;
		}
	}

	public void setBlame(Thread thread) {
		if (this.blame != null) {
			System.out.println("A blame was already assigned: " + this.blame);
			return;
		}

		int threadIndex = getThreadIndex(thread);
		int localTransactionId = this.getTransactionId(threadIndex);
		int localEventId = this.getEventId(threadIndex);
		this.blame = new TransactionEvent(threadIndex, localTransactionId, localEventId, false);
	}

	public TransactionEvent getBlame() {
		return this.blame;
	}

	@Override
	public void printMemory() {
		System.err.println("Number of threads = " + this.numThreads);
		System.err.println("Number of locks tracked = " + this.lockToLastEvent.size());
		System.err.println("Number of variables with reads tracked = " + this.lastReadVariable.size());
		System.err.println("Number of variables with writes tracked = " + this.lastWriteVariable.size());
	}
}
