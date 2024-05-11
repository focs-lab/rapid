package util.ll;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import util.Pair;
import util.Triplet;

public class EfficientLLView<K, V> extends EfficientLinkedList<V> {

	protected HashMap<K, EfficientNode<V>> storeBottomPointerForReader; // READER ->
																		// pointer_in_store
	protected HashMap<K, Integer> storeBottomPointerIndexForReader; // READER ->
																	// 0_based_index__of_reader's_view_from_bottom_of_the_actual_store
	protected HashMap<K, Boolean> storeEmptyForReader; // READER -> Boolean
	private HashSet<K> keys;

	public EfficientLLView(Set<K> keySet) {
		super();
		this.keys = new HashSet<K>(keySet);
		this.storeBottomPointerForReader = new HashMap<K, EfficientNode<V>>();
		this.storeBottomPointerIndexForReader = new HashMap<K, Integer>();
		this.storeEmptyForReader = new HashMap<K, Boolean>();

		for (K key : keySet) {
			this.storeBottomPointerForReader.put(key, null);
			this.storeBottomPointerIndexForReader.put(key, -1);
			this.storeEmptyForReader.put(key, (Boolean) true);
		}
	}

	public void addKeyToBottom(K key) {
		this.keys.add(key);
		if (isEmpty()) {
			this.storeBottomPointerForReader.put(key, null);
			this.storeBottomPointerIndexForReader.put(key, -1);
			this.storeEmptyForReader.put(key, (Boolean) true);
		} else {
			this.storeEmptyForReader.put(key, (Boolean) false);
			this.storeBottomPointerForReader.put(key, getHeadNode());
			this.storeBottomPointerIndexForReader.put(key, 0);
		}
	}

	public void addKeyToTopOfKeys(K key, Set<K> keySubset) {
		this.keys.add(key);
		if (isEmpty()) {
			this.storeBottomPointerForReader.put(key, null);
			this.storeBottomPointerIndexForReader.put(key, -1);
			this.storeEmptyForReader.put(key, (Boolean) true);
		} else {
			boolean keyEmptyReader = true;
			EfficientNode<V> keyPointer = null;
			int keyIndex = -1;
			for (K kprime : keySubset) {
				int kprimeIndex = this.storeBottomPointerIndexForReader.get(kprime);
				if (kprimeIndex > keyIndex) {
					keyIndex = kprimeIndex;
					keyPointer = this.storeBottomPointerForReader.get(kprime);
					keyEmptyReader = false;
				}
			}
			if (keyPointer == null) {
				this.storeEmptyForReader.put(key, (Boolean) false);
				this.storeBottomPointerForReader.put(key, getHeadNode());
				this.storeBottomPointerIndexForReader.put(key, 0);
			} else {
				this.storeEmptyForReader.put(key, keyEmptyReader);
				this.storeBottomPointerForReader.put(key, keyPointer);
				this.storeBottomPointerIndexForReader.put(key, keyIndex);
			}

		}
	}

	public void printKeys() {
		for (K key : this.keys) {
			System.out.println(key.toString() + " @ " + key.hashCode());
		}
	}

	public boolean isEmpty(K key) {
		return this.storeEmptyForReader.get(key);
	}

	public V bottom(K key) {
		if (this.storeEmptyForReader.get(key)) {
			throw new IllegalArgumentException(
					"Cannot get bottom : Store at key '" + key.toString() + "' is empty");
		}
		return this.storeBottomPointerForReader.get(key).getData();
	}

	@Override
	public void pushTop(V val) {
		super.pushTop(val);
		for (K key : this.keys) {
			if (this.storeEmptyForReader.get(key)) {
				this.storeEmptyForReader.put(key, (Boolean) false);
				this.storeBottomPointerForReader.put(key, getHeadNode());
				this.storeBottomPointerIndexForReader.put(key, 0);
			}
		}
	}

	private int getMinIndexOfReaders() {
		// Returns -1 if all readers have empty stores, else returns the index of the
		// minimum bottom pointer of non-empty stores
		int minIndex = -1;
		boolean atLeastOne = false;
		for (K key : this.keys) {
			if (!this.storeEmptyForReader.get(key)) {
				int bottomReaderIndex = this.storeBottomPointerIndexForReader.get(key);
				if (!atLeastOne) {
					atLeastOne = true;
					minIndex = bottomReaderIndex;
				} else {
					if (minIndex > bottomReaderIndex) {
						minIndex = bottomReaderIndex;
					}
				}
			}
		}
		return minIndex;
	}

	private void removeViewPrefixOfLength(int prefixLength) {
		if (this.getLength() < prefixLength) {
			throw new IllegalArgumentException(
					"Invalid operation removeViewPrefixOfLength : Size of store is "
							+ this.getLength() + ", asked to remove : " + prefixLength);
		}
		this.removeBottomPrefixOfLength(prefixLength);
		for (K key : this.keys) {
			if (!this.storeEmptyForReader.get(key)) {
				int minPtr = this.storeBottomPointerIndexForReader.get(key);
				minPtr = minPtr - prefixLength;
				if (minPtr >= 0) {
					this.storeBottomPointerIndexForReader.put(key, minPtr);
				} else {
					this.storeEmptyForReader.put(key, true);
					this.storeBottomPointerForReader.put(key, null);
					this.storeBottomPointerIndexForReader.put(key, -1);
				}
			}
		}
	}

	private void updateStoreToMatchBottomWithMin() {
		EfficientLinkedList<V> st = this;
		int sz = st.getLength();
		if (sz > 0) {
			int minIndex = this.getMinIndexOfReaders();
			if (minIndex >= 0) {
				removeViewPrefixOfLength(minIndex);
			} else {
				removeViewPrefixOfLength(st.getLength());
			}
		}
	}

	public void flush() {
		updateStoreToMatchBottomWithMin();
	}

	// Returns the first node for which node.val > targetVal
	private Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>> getMaxLowerBoundNodePointer(
			K key, V targetVal, Comparator<V> comparator) {
		V maxLowerBound = null;
		boolean node_found = false;
		EfficientNode<V> iter = null;
		int iterIndex = -1;

		if (!this.storeEmptyForReader.get(key)) {
			iter = storeBottomPointerForReader.get(key);
			iterIndex = storeBottomPointerIndexForReader.get(key);
			int totalSize = this.getLength();
			while (iter != null && iterIndex <= totalSize - 1) {
				V data = iter.getData();
				if (comparator.compare(data, targetVal) <= 0) {
					node_found = true;
					maxLowerBound = data;
				} else {
					break;
				}
				iter = iter.getNext();
				iterIndex = iterIndex + 1;
			}
		}
		return new Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>>(node_found,
				maxLowerBound, new Pair<EfficientNode<V>, Integer>(iter, iterIndex));
	}

	// Returns (n1, n2), where n1 is the last node with n1.val <= target and
	// n2 is first node with n2.val > target
	private Triplet<Boolean, V, Pair<Pair<EfficientNode<V>, Integer>, Pair<EfficientNode<V>, Integer>>> getMaxLowerBoundPenultimateNodePointer(
			K key, V targetVal, Comparator<V> comparator) {
		V maxLowerBound = null;
		boolean node_found = false;

		EfficientNode<V> iter = null;
		int iterIndex = -1;
		EfficientNode<V> prev_iter = null;
		int prev_iterIndex = -1;

		if (!this.storeEmptyForReader.get(key)) {
			iter = storeBottomPointerForReader.get(key);
			iterIndex = storeBottomPointerIndexForReader.get(key);
			int totalSize = this.getLength();
			while (iter != null && iterIndex <= totalSize - 1) {
				V data = iter.getData();
				if (comparator.compare(data, targetVal) <= 0) {
					node_found = true;
					maxLowerBound = data;
				} else {
					break;
				}
				prev_iter = iter;
				prev_iterIndex = iterIndex;
				iter = iter.getNext();
				iterIndex = iterIndex + 1;
			}
		}
		Pair<EfficientNode<V>, Integer> prev_node = new Pair<EfficientNode<V>, Integer>(
				prev_iter, prev_iterIndex);
		Pair<EfficientNode<V>, Integer> iter_node = new Pair<EfficientNode<V>, Integer>(
				iter, iterIndex);
		Pair<Pair<EfficientNode<V>, Integer>, Pair<EfficientNode<V>, Integer>> prev_curr = new Pair<Pair<EfficientNode<V>, Integer>, Pair<EfficientNode<V>, Integer>>(
				prev_node, iter_node);
		return new Triplet<Boolean, V, Pair<Pair<EfficientNode<V>, Integer>, Pair<EfficientNode<V>, Integer>>>(
				node_found, maxLowerBound, prev_curr);
	}

	// The store is assumed to be totally ordered according to
	// comparator.compare (denoted <), with the bottom (or headnode) being the
	// smallest
	// and top (or tailNode) being the largest.
	// The function returns a pair <b, val> where b is True if there is
	// at least one node n in the store (corresponding to key) for which
	// n.val <= targetVal. If b is True, then val is the value of the
	// latest node n (with bottom being the earliest) such that n.val <= targetVal.
	// In this case, all the nodes n such that n.val <= targetVal are removed
	// from the store corresponding to key. In this sense, a call to
	// this method can MODIFY the store.
	// If b is false, then val = null.
	//
	// NOTE: The second component is a reference to an object of type V.
	// Care must be taken if this object needs to be updated.
	public Pair<Boolean, V> getMaxLowerBound(K key, V targetVal,
			Comparator<V> comparator) {
		Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>> iter_triplet = this
				.getMaxLowerBoundNodePointer(key, targetVal, comparator);
		boolean node_found = iter_triplet.first;
		V maxLowerBound = iter_triplet.second;
		EfficientNode<V> iter = iter_triplet.third.first;
		int iterIndex = iter_triplet.third.second;

		if (node_found) {
			if (iter != null) {
				this.storeBottomPointerForReader.put(key, iter);
				this.storeBottomPointerIndexForReader.put(key, iterIndex);
			} else {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			}
			this.updateStoreToMatchBottomWithMin();
		} else {
			maxLowerBound = null;
		}
		return new Pair<Boolean, V>(node_found, maxLowerBound);
	}

	public Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>> getMaxLowerBoundPenultimate(
			K key, V targetVal, Comparator<V> comparator) {
		Triplet<Boolean, V, Pair<Pair<EfficientNode<V>, Integer>, Pair<EfficientNode<V>, Integer>>> iter_triplet = this
				.getMaxLowerBoundPenultimateNodePointer(key, targetVal, comparator);
		boolean node_found = iter_triplet.first;
		V maxLowerBound = iter_triplet.second;

		EfficientNode<V> prev_iter = iter_triplet.third.first.first;
		int prev_iterIndex = iter_triplet.third.first.second;

		if (node_found) {
			if (prev_iter != null) {
				this.storeBottomPointerForReader.put(key, prev_iter);
				this.storeBottomPointerIndexForReader.put(key, prev_iterIndex);
			} else {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			}
			// this.updateStoreToMatchBottomWithMin();
		} else {
			maxLowerBound = null;
		}
		return new Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>>(node_found,
				maxLowerBound, iter_triplet.third.second);
	}

	public void setBottom(K key, Pair<EfficientNode<V>, Integer> iter_index) {
		EfficientNode<V> iter = iter_index.first;
		int iterIndex = iter_index.second;
		if (iter != null) {
			this.storeBottomPointerForReader.put(key, iter);
			this.storeBottomPointerIndexForReader.put(key, iterIndex);
		} else {
			this.storeEmptyForReader.put(key, true);
			this.storeBottomPointerForReader.put(key, null);
			this.storeBottomPointerIndexForReader.put(key, -1);
		}
		// this.updateStoreToMatchBottomWithMin();
	}

	// Same as getMaxLowerBound, except that it does not modify the store
	public Pair<Boolean, V> getMaxLowerBoundWithoutUpdate(K key, V targetVal,
			Comparator<V> comparator) {
		Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>> iter_triplet = this
				.getMaxLowerBoundNodePointer(key, targetVal, comparator);
		boolean node_found = iter_triplet.first;
		V maxLowerBound = iter_triplet.second;
		if (!node_found) {
			maxLowerBound = null;
		}
		return new Pair<Boolean, V>(node_found, maxLowerBound);
	}

	// Remove the largest prefix of entries that are between lb and ub.
	// Also return the minimum entry if there is any.
	public Pair<Boolean, V> removePrefixWithinReturnMin(K key, V lb,
			Comparator<V> comparator_lb, V ub, Comparator<V> comparator_ub) {

		V iterVal = null;
		boolean node_found = false;
		EfficientNode<V> iter = null;
		int iterIndex = -1;

		V minVal = null;
		boolean first_one = true;

		if (!this.storeEmptyForReader.get(key)) {
			iter = storeBottomPointerForReader.get(key);
			iterIndex = storeBottomPointerIndexForReader.get(key);
			int totalSize = this.getLength();
			while (iter != null && iterIndex <= totalSize - 1) {
				iterVal = iter.getData();
				if (comparator_lb.compare(lb, iterVal) <= 0
						&& comparator_ub.compare(iterVal, ub) <= 0) {
					node_found = true;
					if (first_one) {
						minVal = iterVal;
						first_one = false;
					}
				} else {
					break;
				}
				iter = iter.getNext();
				iterIndex = iterIndex + 1;
			}
		}

		if (node_found) {
			if (iter != null) {
				this.storeBottomPointerForReader.put(key, iter);
				this.storeBottomPointerIndexForReader.put(key, iterIndex);
			} else {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			}
			this.updateStoreToMatchBottomWithMin();
		} else {
			minVal = null;
		}
		return new Pair<Boolean, V>(node_found, minVal);
	}

	// Remove the largest prefix of entries that are between lb and ub.
	// Also return the minimum entry if there is any.
	public Pair<Boolean, V> removePrefixWithinReturnMin(K key, V lb, V ub,
			Comparator<V> comparator) {
		return removePrefixWithinReturnMin(key, lb, comparator, ub, comparator);
	}

	// Returns null if all keys in keySet have empty stores,
	// else returns the key with the minimum bottom pointer of non-empty stores
	public Pair<K, Integer> getMinKey(HashSet<K> keySet) {
		int minIndex = -1;
		K minKey = null;
		boolean atLeastOne = false;
		for (K key : keySet) {
			if (!this.storeEmptyForReader.get(key)) {
				int bottomReaderIndex = this.storeBottomPointerIndexForReader.get(key);
				if (!atLeastOne) {
					atLeastOne = true;
					minIndex = bottomReaderIndex;
					minKey = key;
				} else {
					if (minIndex > bottomReaderIndex) {
						minIndex = bottomReaderIndex;
						minKey = key;
					}
				}
			}
		}
		return new Pair<K, Integer>(minKey, minIndex);
	}

	// Locate the node n_edge in the store such that n_edge.val <= V but
	// n_edge.next.val > V
	// Now, if any key in keySet is before n_edge, then advance it to point it to
	// n_edge.
	public Pair<Boolean, V> getMaxLowerBoundKeySet(HashSet<K> keySet, V targetVal,
			Comparator<V> comparator) {
		Pair<K, Integer> minKey_minIndex = this.getMinKey(keySet);
		K minKey = minKey_minIndex.first;

		Triplet<Boolean, V, Pair<EfficientNode<V>, Integer>> iter_triplet = this
				.getMaxLowerBoundNodePointer(minKey, targetVal, comparator);
		boolean node_found = iter_triplet.first;
		V maxLowerBound = iter_triplet.second;
		EfficientNode<V> iter = iter_triplet.third.first;
		int iterIndex = iter_triplet.third.second;

		if (node_found) {
			if (iter != null) {
				for (K key : keySet) {
					int keyIndex = this.storeBottomPointerIndexForReader.get(key);
					if (keyIndex < iterIndex) {
						this.storeBottomPointerForReader.put(key, iter);
						this.storeBottomPointerIndexForReader.put(key, iterIndex);
					}
				}
			} else {
				for (K key : keySet) {
					this.storeEmptyForReader.put(key, true);
					this.storeBottomPointerForReader.put(key, null);
					this.storeBottomPointerIndexForReader.put(key, -1);
				}
			}
			this.updateStoreToMatchBottomWithMin();
		} else {
			maxLowerBound = null;
		}
		return new Pair<Boolean, V>(node_found, maxLowerBound);
	}

	public void advanceKeyByOne(K key) {
		if (!this.storeEmptyForReader.get(key)) {
			EfficientNode<V> iter = storeBottomPointerForReader.get(key);
			int keyIndex = storeBottomPointerIndexForReader.get(key);
			if (keyIndex == this.getLength() - 1) {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			} else {
				keyIndex = keyIndex + 1;
				iter = iter.getNext();
				this.storeBottomPointerForReader.put(key, iter);
				this.storeBottomPointerIndexForReader.put(key, keyIndex);
			}
		}
	}

	// If key is earlier than target, then advance it to target.
	public void advanceKeyToTarget(K key, K target) {
		if (!this.storeEmptyForReader.get(key)) {
			if (this.storeEmptyForReader.get(target)) {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			} else {
				int key_index = storeBottomPointerIndexForReader.get(key);
				int target_index = storeBottomPointerIndexForReader.get(target);
				if (key_index < target_index) {
					EfficientNode<V> tagret_iter = storeBottomPointerForReader
							.get(target);
					this.storeBottomPointerForReader.put(key, tagret_iter);
					this.storeBottomPointerIndexForReader.put(key, target_index);
				}
			}
		}
	}

	// If givenKey's view is nonempty - for every key: keySet, if key is behind
	// givenKey, then advance it to givenKey.
	// If however given key is empty, then set all to empty
	public void setBottomOfAllKeysToGivenKey(HashSet<K> keySet, K givenKey) {
		if (this.storeEmptyForReader.get(givenKey)) {
			for (K key : keySet) {
				this.storeEmptyForReader.put(key, true);
				this.storeBottomPointerForReader.put(key, null);
				this.storeBottomPointerIndexForReader.put(key, -1);
			}
		} else {
			EfficientNode<V> givenKey_iter = storeBottomPointerForReader.get(givenKey);
			int givenkey_index = storeBottomPointerIndexForReader.get(givenKey);
			for (K key : keySet) {
				int key_index = storeBottomPointerIndexForReader.get(key);
				if (key_index < givenkey_index) {
					this.storeBottomPointerForReader.put(key, givenKey_iter);
					this.storeBottomPointerIndexForReader.put(key, givenkey_index);
				}
			}
		}
	}

	public int getSize() {
		return getLength();
	}

	public void printSize() {
		System.err.println("Size = " + Integer.toString(this.getSize()));
	}

	@Override
	public String toString() {
		String store = super.toString();
		String keyBottoms = "[ ";
		for (K key : this.keys) {
			keyBottoms += "<";
			keyBottoms += key.toString();
			keyBottoms += " : ";
			keyBottoms += this.storeEmptyForReader.get(key) ? "{_|_}" : bottom(key);
			keyBottoms += "> | \n";
		}
		keyBottoms += " ]";
		return store + "\n" + keyBottoms;
	}

	public String toStoreString() {
		return super.toString();
	}

	public void destroyKey(K key) {
		if (!keys.contains(key)) {
			throw new IllegalArgumentException(
					"Cannot delete store for non-existent key " + key.toString());
		} else {
			this.storeBottomPointerForReader.remove(key);
			this.storeEmptyForReader.remove(key);
			this.storeBottomPointerIndexForReader.remove(key);
			this.updateStoreToMatchBottomWithMin();
		}
	}

}