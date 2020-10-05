package engine.racedetectionengine.wcp;

import java.util.HashMap;
import java.util.HashSet;
///import java.util.ListIterator;

import event.Lock;
import event.Thread;
import util.ll.EfficientLinkedList;
import util.ll.EfficientNode;
import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

public class WCPView {

	//private HashMap<Lock, HashMap<Thread, EfficientStore>> view; // (LOCK, index of WRITER) -> Store
	private HashMap<Lock, EfficientLinkedList<ClockPair>> view; // LOCK -> Store
	private HashMap<Lock, HashMap<Thread, EfficientNode<ClockPair>>> stackBottomPointerForReader; //LOCK -> (READER -> pointer_in_stack)
	private HashMap<Lock, HashMap<Thread, Integer>> stackBottomPointerIndexForReader; //LOCK -> (READER -> 0_based_index__of_reader's_view_from_bottom_of_the_actual_stack)
	private HashMap<Lock, HashMap<Thread, Boolean>> stackEmptyForReader; //LOCK -> (READER -> Boolean)	
	private HashSet<Thread> threadSet;
	private HashSet<Lock> lockSet;
	
	public HashMap<String, HashMap<String, Long>> lockThreadLastInteraction;
	
	//private VectorClock tempMin;

	WCPView(HashSet<Thread> tSet) {
		this.threadSet = new HashSet<Thread>(tSet);
		this.lockSet = new HashSet<Lock> ();		
		this.view = new HashMap<Lock, EfficientLinkedList<ClockPair>>();
		this.stackBottomPointerForReader = new HashMap<Lock, HashMap<Thread, EfficientNode<ClockPair>>>();		
		this.stackBottomPointerIndexForReader = new HashMap<Lock, HashMap<Thread, Integer>>();
		this.stackEmptyForReader = new HashMap<Lock, HashMap<Thread, Boolean>>();
		//this.tempMin = new VectorClock(this.threadSet.size()) ;
	}
	
	public void checkAndAddLock(Lock l){
		if(!lockSet.contains(l)){
			
			lockSet.add(l);			
			view.put(l, new EfficientLinkedList<ClockPair>());
			stackBottomPointerForReader.put(l, new HashMap<Thread, EfficientNode<ClockPair>> ());
			stackBottomPointerIndexForReader.put(l, new HashMap<Thread, Integer> ());
			stackEmptyForReader.put(l, new HashMap<Thread, Boolean> ());

			for(Thread tReader : this.threadSet){
				if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
					stackBottomPointerForReader.get(l).put(tReader, null );
					stackBottomPointerIndexForReader.get(l).put(tReader, -1 );
					stackEmptyForReader.get(l).put(tReader, (Boolean)true);	

				}						
			}	

		}
		//return lockToIndex.get(l);
	}

	/*
	public void pushClockPair(Thread tWriter, Lock l, WCPClockPair clockPair) {
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		//System.out.println("Pushing to stack : (l, tWriter) = (" + l + ", " + tWriter + ")" );
		this.view.get(l).pushTop(clockPair);
		for(Thread tReader : this.threadSet){
			if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
				if(this.stackEmptyForReader.get(l).get(tReader)){
					this.stackEmptyForReader.get(l).put(tReader, (Boolean)false);
					this.stackBottomPointerForReader.get(l).put(tReader, view.get(l).getheadNode());
					this.stackBottomPointerIndexForReader.get(l).put(tReader, 0);
				}
			}	

		}
	}
	*/

	public void pushClockPair(Lock l, ClockPair clockPair) {
		checkAndAddLock(l);
		//System.out.println("Pushing to stack : (l, tWriter) = (" + l + ", " + tWriter + ")" );
		this.view.get(l).pushTop(clockPair);
		for(Thread tReader : this.threadSet){
			if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
				if(this.stackEmptyForReader.get(l).get(tReader)){
					this.stackEmptyForReader.get(l).put(tReader, (Boolean)false);
					this.stackBottomPointerForReader.get(l).put(tReader, view.get(l).getHeadNode());
					this.stackBottomPointerIndexForReader.get(l).put(tReader, 0);
				}
			}	

		}
	}

	/*
	private int getMinIndexOfReaders(Thread tWriter, Lock l){
		//Precondition: Stack at (l, tWriter) is non-empty
		//Returns -1 if all readers have empty stacks, else returns the index of the minimum bottom pointer of non-empty stacks
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		int minIndex = -1;		
		boolean atLeastOne = false;
		for(Thread tReader : this.threadSet){
			if(tWriter.getId() != tReader.getId()){
				if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
					if( ! this.stackEmptyForReader.get(l).get(tReader)){
						int bottomReaderIndex = this.stackBottomPointerIndexForReader.get(l).get(tReader);
						if(! atLeastOne){
							atLeastOne = true;
							minIndex = bottomReaderIndex;
						}
						else{
							if(minIndex > bottomReaderIndex){
								minIndex = bottomReaderIndex;
							}
						}
					}
				}	
			}			
		}
		return minIndex;
	}
	*/
	
	private int getMinIndexOfReaders(Lock l){
		//Precondition: Stack at (l, tWriter) is non-empty
		//Returns -1 if all readers have empty stacks, else returns the index of the minimum bottom pointer of non-empty stacks
		checkAndAddLock(l);
		int minIndex = -1;		
		boolean atLeastOne = false;
		for(Thread tReader : this.threadSet){
			if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
				if( ! this.stackEmptyForReader.get(l).get(tReader)){
					int bottomReaderIndex = this.stackBottomPointerIndexForReader.get(l).get(tReader);
					if(! atLeastOne){
						atLeastOne = true;
						minIndex = bottomReaderIndex;
					}
					else{
						if(minIndex > bottomReaderIndex){
							minIndex = bottomReaderIndex;
						}
					}
				}
			}	

		}
		return minIndex;
	}
	
	/*
	private void removeViewPrefixOfLength (Thread tWriter, Lock l, int prefixLength){
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		if(this.view.get(l).getLength() < prefixLength){
			throw new IllegalArgumentException("Invalid operation removeViewPrefixOfLength : Size of stack at (" + l.getName() +  ") is " + this.view.get(l).getLength() + ", asked to remove : " + prefixLength );
		}
		this.view.get(l).removeBottomPrefixOfLength(prefixLength);
		for(Thread tReader : this.threadSet){
			if(tWriter.getId() != tReader.getId()){
				if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
					if( ! this.stackEmptyForReader.get(l).get(tReader)){
						int minPtr = this.stackBottomPointerIndexForReader.get(l).get(tReader);
						minPtr = minPtr - prefixLength;
						if(minPtr >= 0){
							this.stackBottomPointerIndexForReader.get(l).put(tReader, minPtr);
						}
						else{
							this.stackEmptyForReader.get(l).put(tReader, true);
							this.stackBottomPointerForReader.get(l).put(tReader, null);
							this.stackBottomPointerIndexForReader.get(l).put(tReader, -1);
						}
					}
				}	
			}			
		}
	}
	*/
	
	private void removeViewPrefixOfLength (Lock l, int prefixLength){
		if(this.view.get(l).getLength() < prefixLength){
			throw new IllegalArgumentException("Invalid operation removeViewPrefixOfLength : Size of stack at (" + l.getName() +  ") is " + this.view.get(l).getLength() + ", asked to remove : " + prefixLength );
		}
		this.view.get(l).removeBottomPrefixOfLength(prefixLength);
		for(Thread tReader : this.threadSet){
			if(this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
				if( ! this.stackEmptyForReader.get(l).get(tReader)){
					int minPtr = this.stackBottomPointerIndexForReader.get(l).get(tReader);
					minPtr = minPtr - prefixLength;
					if(minPtr >= 0){
						this.stackBottomPointerIndexForReader.get(l).put(tReader, minPtr);
					}
					else{
						this.stackEmptyForReader.get(l).put(tReader, true);
						this.stackBottomPointerForReader.get(l).put(tReader, null);
						this.stackBottomPointerIndexForReader.get(l).put(tReader, -1);
					}
				}
			}	

		}
	}
	
	/*
	private void updateStoreToMatchBottomWithMin(Thread tWriter, Lock l){
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		EfficientStore st = this.view.get(l);
		int sz = st.getLength();
		if(sz > 0){
			//int minIndex = this.getMinIndexOfReaders(tWriter, l); 
			int minIndex = this.getMinIndexOfReaders(l);
			if(minIndex >= 0){
				//removeViewPrefixOfLength(tWriter, l, minIndex);
				removeViewPrefixOfLength(l, minIndex);
			}
			else{
				//removeViewPrefixOfLength(tWriter, l, st.getLength());
				removeViewPrefixOfLength(l, st.getLength());
				
			}
		}
	}
	*/
	
	private void updateStoreToMatchBottomWithMin(Lock l){
		checkAndAddLock(l);
		EfficientLinkedList<ClockPair> st = this.view.get(l);
		int sz = st.getLength();
		if(sz > 0){ 
			int minIndex = this.getMinIndexOfReaders(l);
			if(minIndex >= 0){
				removeViewPrefixOfLength(l, minIndex);
			}
			else{
				removeViewPrefixOfLength(l, st.getLength());
				
			}
		}
	}

	/*
	// result is going to be overwritten with the release of the largest acq <= ct
	public void getMaxLowerBound(Thread tWriter, Thread tReader, Lock l, VectorClock ct, VectorClock result) {
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tReader.getName());
		}
		
		result.setToZero();
		WCPClockPair clockPair = null;
		boolean pairFound = false;
		EfficientNode lIter = null;
		int lIterIndex = -1;
		
		if(! this.stackEmptyForReader.get(l).get(tReader)){
			//int index = this.view.get(l).get(tWriter).getIndexOfAcquire(readerStackBottomPointer.get(l).get(tWriter).get(tReader));
			lIter = stackBottomPointerForReader.get(l).get(tReader);
			lIterIndex = stackBottomPointerIndexForReader.get(l).get(tReader);
			while(lIter != null){
				clockPair = lIter.getData();
				VectorClock acquireClock = clockPair.getAcquire();
				if (acquireClock.isLessThanOrEqual(ct)) {
					pairFound = true;
				} else {
					break;
				}
				lIter = lIter.getNext();
				lIterIndex = lIterIndex + 1;
			}
		}
		
		if(pairFound){
			result.copyFrom(clockPair.getRelease());
			if(lIter != null){
				this.stackBottomPointerForReader.get(l).put(tReader, lIter);
				this.stackBottomPointerIndexForReader.get(l).put(tReader, lIterIndex);
			}
			else{
				this.stackEmptyForReader.get(l).put(tReader,true);
				this.stackBottomPointerForReader.get(l).put(tReader, null);
				this.stackBottomPointerIndexForReader.get(l).put(tReader, -1);
			}
			//this.updateStoreToMatchBottomWithMin(tWriter, l);
			this.updateStoreToMatchBottomWithMin(l);
		}
	}
	*/
	
	// result is going to be overwritten with the release of the largest acq <= ct
	public void getMaxLowerBound(Thread tReader, Lock l, VectorClock ct, VectorClock result) {
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tReader.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tReader.getName());
		}		
		result.setToZero();
		
		ClockPair clockPair = null;
		boolean pairFound = false;
		EfficientNode<ClockPair> lIter = null;
		int lIterIndex = -1;
		
		if(! this.stackEmptyForReader.get(l).get(tReader)){
			lIter = stackBottomPointerForReader.get(l).get(tReader);
			lIterIndex = stackBottomPointerIndexForReader.get(l).get(tReader);
			int totalSize = this.view.get(l).getLength(); 
			while(lIter != null && lIterIndex < totalSize - 1){
				clockPair = lIter.getData();
				VectorClock acquireClock = clockPair.getAcquire();
				if (acquireClock.isLessThanOrEqual(ct)) {
					pairFound = true;
				} else {
					break;
				}
				result.updateWithMax(result, clockPair.getRelease());
				lIter = lIter.getNext();
				lIterIndex = lIterIndex + 1;
			}
		}
		
		if(pairFound){
			//result.copyFrom(clockPair.getRelease());
			if(lIter != null){
				this.stackBottomPointerForReader.get(l).put(tReader, lIter);
				this.stackBottomPointerIndexForReader.get(l).put(tReader, lIterIndex);
			}
			else{
				this.stackEmptyForReader.get(l).put(tReader,true);
				this.stackBottomPointerForReader.get(l).put(tReader, null);
				this.stackBottomPointerIndexForReader.get(l).put(tReader, -1);
			}
			//this.updateStoreToMatchBottomWithMin(tWriter, l);
			this.updateStoreToMatchBottomWithMin(l);
		}
	}
	
	/*
	public void updateTopRelease(Thread tWriter, Lock l, VectorClock ct, VectorClock ht) {
		checkAndAddLock(l);
		if(!this.lockThreadLastInteraction.get(l.getName()).containsKey(tWriter.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + tWriter.getName());
		}
		//System.out.println("(tWriter, l) = (" + tWriter + ", " + l + ")" );
		//System.out.println(this.view);
		WCPClockPair clockPair = this.view.get(l).top();
		clockPair.getRelease().updateWithMax(ct, ht);
	}
	*/
	
	public void updateTopRelease(Lock l, VectorClock ct, VectorClock ht) {
		checkAndAddLock(l);
		//System.out.println("(tWriter, l) = (" + tWriter + ", " + l + ")" );
		//System.out.println(this.view);
		ClockPair clockPair = this.view.get(l).top();
		clockPair.getRelease().updateWithMax(ct, ht);
	}
	
	public String toString(){
		String str = "";
		for(Lock l: lockSet){
			str += "[" + l.getName() + "]";
			str += " : " + view.get(l).toString() + "\n";	
		}
		str += "\n";
		return str;
	}
	
	public int getSize(){
		int sz = 0;
		for(Lock l: lockSet){
			sz += view.get(l).getLength();	
		}
		return sz;
	}
	
	public void printSize(){
		System.err.println("Stack size = " + Integer.toString(this.getSize()));
	}
	
	public void destroyLock(Lock l){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete non-existent lock " + l.getName());
		}
		else{
			lockSet.remove(l);
			view.remove(l);
			this.stackBottomPointerForReader.remove(l);
			this.stackBottomPointerIndexForReader.remove(l);
			this.stackEmptyForReader.remove(l);
		}
	}
	
	public void destroyLockThreadStack(Lock l, Thread t){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete stack for non-existent lock " + l.getName());
		}
		else if(!threadSet.contains(t)){
			throw new IllegalArgumentException("Cannot delete stack for non-existent thread " + t.getName());
		}
		else{
			if(this.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
				this.stackBottomPointerForReader.get(l).remove(t);
				this.stackEmptyForReader.get(l).remove(t);
				this.stackBottomPointerIndexForReader.get(l).remove(t);
				this.lockThreadLastInteraction.get(l.getName()).remove(t.getName());
				updateStoreToMatchBottomWithMin(l);
			}

		}
	}
	
	

}