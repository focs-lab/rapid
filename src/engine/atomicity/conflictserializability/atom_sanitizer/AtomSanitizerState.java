package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;

public class AtomSanitizerState extends State {
	private static final int BOTTOM_TRANSACTION_ID = -1;

	public static boolean VERBOSE = false;

	public final HashMap<Thread, Integer> threadToIndex;
	private final int numThreads;
	private final int[] threadToCurrentTransaction;
	private final int[] threadToLocalTransactionCount;
	private final int[] threadToNestingDepth;

	public final HashMap<Lock, SimpleTransaction> lockToLastReleaseTransaction;
	public final HashMap<Variable, SimpleTransaction[]> readVariableThreadToTransaction;
	public final HashMap<Variable, SimpleTransaction> writeVariableToTransaction;

	private final IncrementalSimpleMaxCSSTs cssts;

	public AtomSanitizerState(HashSet<Thread> threadSet, int verbosity) {
		this.verbosity = verbosity;
		this.threadToIndex = new HashMap<Thread, Integer>();
		int threadIndex = 0;
		Iterator<Thread> threadIterator = threadSet.iterator();
		while (threadIterator.hasNext()) {
			this.threadToIndex.put(threadIterator.next(), threadIndex);
			threadIndex++;
		}

		this.numThreads = threadIndex;
		this.threadToCurrentTransaction = new int[numThreads];
		this.threadToLocalTransactionCount = new int[numThreads];
		this.threadToNestingDepth = new int[numThreads];
		for (int i = 0; i < numThreads; i++) {
			this.threadToCurrentTransaction[i] = BOTTOM_TRANSACTION_ID;
			this.threadToLocalTransactionCount[i] = 0;
			this.threadToNestingDepth[i] = 0;
		}

		this.lockToLastReleaseTransaction = new HashMap<Lock, SimpleTransaction>();
		this.readVariableThreadToTransaction = new HashMap<Variable, SimpleTransaction[]>();
		this.writeVariableToTransaction = new HashMap<Variable, SimpleTransaction>();
		this.cssts = new IncrementalSimpleMaxCSSTs(numThreads);
	}

	public static boolean isBottomTransaction(int transactionId) {
		return transactionId == BOTTOM_TRANSACTION_ID;
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

	public int getTransactionId(int threadIndex) {
		return this.threadToCurrentTransaction[threadIndex];
	}

	public int getFreshTransactionId(int threadIndex) {
		int nextTransactionId = this.threadToLocalTransactionCount[threadIndex];
		this.threadToLocalTransactionCount[threadIndex]++;
		return nextTransactionId;
	}

	public boolean checkCycleAndAddPath(int fromThread, int fromTransactionId, int toThread, int toTransactionId) {
		if (isBottomTransaction(fromTransactionId) || isBottomTransaction(toTransactionId) || fromThread == toThread) {
			return false;
		}

		boolean foundCycle = this.cssts.reachable(toThread, toTransactionId, fromThread, fromTransactionId);
		if (!foundCycle) {
			this.addPath(fromThread, fromTransactionId, toThread, toTransactionId, false);
		}
		return foundCycle;
	}

	public void addPath(int fromThread, int fromTransactionId, int toThread, int toTransactionId, boolean backwardOnly) {
		if (fromThread == toThread) {
			return;
		}

		if (backwardOnly) {
			this.cssts.insertEdgeWithOnlyBackwardClosure(fromThread, fromTransactionId, toThread, toTransactionId);
		} else {
			this.cssts.insertEdge(fromThread, fromTransactionId, toThread, toTransactionId);
		}

		if (VERBOSE) {
			System.out.println(String.format("addPath: (%d, %d) -> (%d, %d)", fromThread, fromTransactionId, toThread,
					toTransactionId));
			this.cssts.print();
		}
	}

	public void setLastRead(Variable variable, int threadIndex, int transactionId) {
		SimpleTransaction[] readList = this.readVariableThreadToTransaction.get(variable);
		if (readList == null) {
			readList = new SimpleTransaction[this.numThreads];
			this.readVariableThreadToTransaction.put(variable, readList);
		}

		SimpleTransaction previous = readList[threadIndex];
		if (previous == null) {
			readList[threadIndex] = new SimpleTransaction(threadIndex, transactionId);
		} else {
			previous.update(threadIndex, transactionId);
		}
	}

	public void setLastWrite(Variable variable, int threadIndex, int transactionId) {
		SimpleTransaction previous = this.writeVariableToTransaction.get(variable);
		if (previous == null) {
			this.writeVariableToTransaction.put(variable, new SimpleTransaction(threadIndex, transactionId));
		} else {
			previous.update(threadIndex, transactionId);
		}
	}

	public void setLastLockRelease(Lock lock, int threadIndex, int transactionId) {
		SimpleTransaction previous = this.lockToLastReleaseTransaction.get(lock);
		if (previous == null) {
			this.lockToLastReleaseTransaction.put(lock, new SimpleTransaction(threadIndex, transactionId));
		} else {
			previous.update(threadIndex, transactionId);
		}
	}

	public void clearLastReads(Variable variable) {
		if (this.readVariableThreadToTransaction.containsKey(variable)) {
			this.readVariableThreadToTransaction.put(variable, null);
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

	@Override
	public void printMemory() {
		System.err.println("Number of threads = " + this.numThreads);
		System.err.println("Number of locks tracked = " + this.lockToLastReleaseTransaction.size());
		System.err.println("Number of variables with reads tracked = " + this.readVariableThreadToTransaction.size());
		System.err.println("Number of variables with writes tracked = " + this.writeVariableToTransaction.size());
	}
}
