package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.Arrays;

final class IncrementalSimpleMaxCSSTs {
	private static final boolean VERBOSE = false;
	private static final int NO_ENTRY_FROM = Integer.MIN_VALUE;
	private static final int NO_ENTRY_TO = -1;

	private final int width;
	// Paper notation:
	// fromValues[toThread][fromThread] = From_toThread(fromThread)
	// toValues[toThread][fromThread] = To_toThread(fromThread)
	private final int[][] fromValues;
	private final int[][] toValues;

	IncrementalSimpleMaxCSSTs(int numThreads) {
		this.width = numThreads;
		this.fromValues = new int[width][width];
		this.toValues = new int[width][width];
		for (int toThread = 0; toThread < width; toThread++) {
			Arrays.fill(this.fromValues[toThread], NO_ENTRY_FROM);
			Arrays.fill(this.toValues[toThread], NO_ENTRY_TO);
		}
	}

	private int getSuccessor(int fromThread, int fromTransactionId, int targetThread) {
		if (VERBOSE) {
			System.out.println(
					String.format("getSuccessor. p: <%d, %d> i: %d", fromThread, fromTransactionId, targetThread));
		}

		if (fromThread == targetThread) {
			return fromTransactionId + 1;
		}

		if (this.fromValues[targetThread][fromThread] >= fromTransactionId) {
			return this.toValues[targetThread][fromThread];
		}
		return -1;
	}

	private int getPredecessor(int fromThread, int fromTransactionId, int targetThread) {
		if (VERBOSE) {
			System.out.println(String.format("getPredecessor. p: <%d, %d> i: %d", fromThread, fromTransactionId,
					targetThread));
		}

		if (fromThread == targetThread) {
			return fromTransactionId > 0 ? fromTransactionId - 1 : -1;
		}

		if (this.toValues[fromThread][targetThread] != NO_ENTRY_TO
				&& this.toValues[fromThread][targetThread] <= fromTransactionId) {
			return this.fromValues[fromThread][targetThread];
		}
		return -1;
	}

	boolean reachable(int fromThread, int fromTransactionId, int toThread, int toTransactionId) {
		if (VERBOSE) {
			System.out.println(String.format("reachable <%d, %d> -> <%d, %d>", fromThread, fromTransactionId, toThread,
					toTransactionId));
		}

		if (fromThread == toThread && fromTransactionId < toTransactionId) {
			return true;
		}

		int successor = getSuccessor(fromThread, fromTransactionId, toThread);
		return successor >= 0 && successor <= toTransactionId;
	}

	private void addSuccessor(int fromThread, int fromTransactionId, int toThread, int toTransactionId) {
		if (fromThread == toThread) {
			return;
		}

		if (this.fromValues[toThread][fromThread] < fromTransactionId) {
			this.fromValues[toThread][fromThread] = fromTransactionId;
			this.toValues[toThread][fromThread] = toTransactionId;
		} else if (this.fromValues[toThread][fromThread] == fromTransactionId
				&& (this.toValues[toThread][fromThread] == NO_ENTRY_TO
						|| this.toValues[toThread][fromThread] > toTransactionId)) {
			this.toValues[toThread][fromThread] = toTransactionId;
		}
	}

	void insertEdge(int fromThread, int fromTransactionId, int toThread, int toTransactionId) {
		if (this.reachable(fromThread, fromTransactionId, toThread, toTransactionId)) {
			return;
		}

		for (int predecessorThread = 0; predecessorThread < width; predecessorThread++) {
			int predecessorTransactionId;
			if (predecessorThread != fromThread) {
				predecessorTransactionId = this.getPredecessor(fromThread, fromTransactionId, predecessorThread);
				if (predecessorTransactionId < 0) {
					continue;
				}
			} else {
				predecessorTransactionId = fromTransactionId;
			}

			for (int successorThread = 0; successorThread < width; successorThread++) {
				if (predecessorThread == successorThread) {
					continue;
				}

				int successorTransactionId;
				if (successorThread != toThread) {
					successorTransactionId = this.getSuccessor(toThread, toTransactionId, successorThread);
					if (successorTransactionId < 0) {
						continue;
					}
				} else {
					successorTransactionId = toTransactionId;
				}

				if (!this.reachable(predecessorThread, predecessorTransactionId, successorThread,
						successorTransactionId)) {
					this.addSuccessor(predecessorThread, predecessorTransactionId, successorThread,
							successorTransactionId);
				}
			}
		}
	}

	void insertEdgeWithOnlyBackwardClosure(int fromThread, int fromTransactionId, int toThread, int toTransactionId) {
		if (this.reachable(fromThread, fromTransactionId, toThread, toTransactionId)) {
			return;
		}

		for (int predecessorThread = 0; predecessorThread < width; predecessorThread++) {
			int predecessorTransactionId;
			if (predecessorThread != fromThread) {
				predecessorTransactionId = this.getPredecessor(fromThread, fromTransactionId, predecessorThread);
				if (predecessorTransactionId < 0) {
					continue;
				}
			} else {
				predecessorTransactionId = fromTransactionId;
			}

			if (!this.reachable(predecessorThread, predecessorTransactionId, toThread, toTransactionId)) {
				this.addSuccessor(predecessorThread, predecessorTransactionId, toThread, toTransactionId);
			}
		}
	}

	void insertEdgeWithOnlyBackwardClosureSpecial(int fromThread, int fromTransactionIdToAdd,
			int fromTransactionIdForPred, int toThread, int toTransactionId) {
		if (this.reachable(fromThread, fromTransactionIdForPred, toThread, toTransactionId)) {
			return;
		}

		for (int predecessorThread = 0; predecessorThread < width; predecessorThread++) {
			int predecessorTransactionId;
			if (predecessorThread != fromThread) {
				predecessorTransactionId = this.getPredecessor(fromThread, fromTransactionIdForPred,
						predecessorThread);
				if (predecessorTransactionId < 0) {
					continue;
				}
			} else {
				predecessorTransactionId = fromTransactionIdToAdd;
			}

			if (!this.reachable(predecessorThread, predecessorTransactionId, toThread, toTransactionId)) {
				this.addSuccessor(predecessorThread, predecessorTransactionId, toThread, toTransactionId);
			}
		}
	}

	void print() {
		for (int fromThread = 0; fromThread < width; fromThread++) {
			for (int toThread = 0; toThread < width; toThread++) {
				if (fromThread == toThread) {
					continue;
				}
				System.out.println(String.format("Entry (%d -> %d): from=%d, to=%d", fromThread, toThread,
						this.fromValues[toThread][fromThread], this.toValues[toThread][fromThread]));
			}
		}
	}
}
