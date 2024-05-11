package engine.racedetectionengine.lockset;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Lock;

public class LockSetEvent extends RaceDetectionEvent<LockSetState> {

	@Override
	public boolean Handle(LockSetState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(LockSetState state, int verbosity) {
		System.out.println("Dummy method called");		
	}

	@Override
	public void printRaceInfoAccessType(LockSetState state, int verbosity) {
		System.out.println("Dummy method called");	
	}

	@Override
	public void printRaceInfoExtremeType(LockSetState state, int verbosity) {
		System.out.println("Dummy method called");
	}

	@Override
	public boolean HandleSubAcquire(LockSetState state, int verbosity) {
		int l = 1;
		if(state.locksHeldNesting.get(this.getThread()).containsKey(this.getLock())){
			l = 1 + state.locksHeldNesting.get(this.getThread()).get(this.getLock());
		}
		state.locksHeldNesting.get(this.getThread()).put(this.getLock(), l);
		state.locksHeldSet.get(this.getThread()).add(this.getLock());
		return false;
	}

	@Override
	public boolean HandleSubRelease(LockSetState state, int verbosity) {
		int l;
		if(state.locksHeldNesting.get(this.getThread()).containsKey(this.getLock())){
			l = state.locksHeldNesting.get(this.getThread()).get(this.getLock()) - 1;
		}
		else{
			throw new IllegalArgumentException("Thread " + this.getThread().getName() + " is releasing lock " + this.getLock().getName() + " without acquiring it enough number of times .");
		}
		state.locksHeldNesting.get(this.getThread()).put(this.getLock(), l);
		if(l == 0){
			state.locksHeldSet.get(this.getThread()).remove(this.getLock());
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(LockSetState state, int verbosity) {
		boolean raceDetected = false;
		if(!state.lockSet.containsKey(this.getVariable())){
			state.lockSet.put(getVariable(), new HashSet<Lock> (state.locksHeldSet.get(this.getThread())));
			state.lockSet.get(this.getVariable()).add(state.dummyReadLock);
		}
		if(state.lockSet.get(this.getVariable()).contains(state.dummyReadLock)){
			state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
			state.lockSet.get(this.getVariable()).add(state.dummyReadLock);
		}
		else{
			state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
		}
		if(state.lockSet.get(this.getVariable()).isEmpty()){
			raceDetected = true;
		}
		if(raceDetected){
			System.out.println("LockSet discipline violated on variable " + this.getVariable().getName());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(LockSetState state, int verbosity) {
		boolean raceDetected = false;
		if(!state.lockSet.containsKey(this.getVariable())){
			state.lockSet.put(getVariable(), new HashSet<Lock> (state.locksHeldSet.get(this.getThread())));
		}
		state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
		if(state.lockSet.get(this.getVariable()).isEmpty()){
			raceDetected = true;
		}
		if(raceDetected){
			System.out.println("LockSet discipline violated on variable " + this.getVariable().getName());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(LockSetState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubJoin(LockSetState state, int verbosity) {
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(LockSetState state, int verbosity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean HandleSubBegin(LockSetState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(LockSetState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
