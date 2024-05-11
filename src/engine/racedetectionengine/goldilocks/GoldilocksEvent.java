package engine.racedetectionengine.goldilocks;

import java.util.HashMap;
import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Thread;
import event.Lock;
import event.Variable;

public class GoldilocksEvent extends RaceDetectionEvent<GoldilocksState> {

	@Override
	public boolean Handle(GoldilocksState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(GoldilocksState state, int verbosity) {
		System.out.println("Dummy method called");		
	}

	@Override
	public void printRaceInfoAccessType(GoldilocksState state, int verbosity) {
		System.out.println("Dummy method called");	
	}

	@Override
	public void printRaceInfoExtremeType(GoldilocksState state, int verbosity) {
		System.out.println("Dummy method called");
	}

	@Override
	public boolean HandleSubAcquire(GoldilocksState state, int verbosity) {
		for(Variable x: state.writeLockSet.keySet()){
			if(state.writeLockSet.get(x).contains(this.getLock())){
				state.writeLockSet.get(x).add(state.threadLocks.get(this.getThread())) ;
			}
		}
		for(Thread t: state.threadLocks.keySet()){
			Lock tLock = state.threadLocks.get(t);
			if(state.readLockSet.containsKey(tLock)){
				for(Variable x: state.readLockSet.get(tLock).keySet()){
					if(state.readLockSet.get(tLock).get(x).contains(this.getLock())){
						state.readLockSet.get(tLock).get(x).add(state.threadLocks.get(this.getThread())) ;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRelease(GoldilocksState state, int verbosity) {
		for(Variable x: state.writeLockSet.keySet()){
			if(state.writeLockSet.get(x).contains(state.threadLocks.get(this.getThread()))){
				state.writeLockSet.get(x).add(this.getLock()) ;
			}
		}
		for(Thread t: state.threadLocks.keySet()){
			Lock tLock = state.threadLocks.get(t);
			if(state.readLockSet.containsKey(tLock)){
				for(Variable x: state.readLockSet.get(tLock).keySet()){
					if(state.readLockSet.get(tLock).get(x).contains(state.threadLocks.get(this.getThread()))){
						state.readLockSet.get(tLock).get(x).add(this.getLock()) ;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(GoldilocksState state, int verbosity) {
		boolean raceDetected = false;
		
		Thread t = this.getThread();
		Variable x = this.getVariable();
		Lock tLock = state.threadLocks.get(t);
		
		if(state.writeLockSet.containsKey(x)){
			if(!state.writeLockSet.get(x).contains(tLock)){
				raceDetected = true;
			}
		}
		if(raceDetected){
			System.out.println("Goldilocks algorithm detected a race");
		}
		
		if(!state.readLockSet.containsKey(tLock)){
			state.readLockSet.put(tLock, new HashMap<Variable, HashSet<Lock>> ());
		}
		state.readLockSet.get(tLock).put(x, new HashSet<Lock> ());
		state.readLockSet.get(tLock).get(x).add(tLock);
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(GoldilocksState state, int verbosity) {
		boolean raceDetected = false;
		
		Thread t = this.getThread();
		Variable x = this.getVariable();
		Lock tLock = state.threadLocks.get(t);
		
		if(state.writeLockSet.containsKey(x)){
			if(!state.writeLockSet.get(x).contains(tLock)){
				raceDetected = true;
			}
		}
		for(Thread u: state.threadLocks.keySet()){
			Lock uLock = state.threadLocks.get(u);
			if(state.readLockSet.containsKey(uLock)){
				if(state.readLockSet.get(uLock).containsKey(x)){
					if(!state.readLockSet.get(uLock).get(x).contains(tLock)){
						raceDetected = true;
					}
				}
			}
		}
		
		if(raceDetected){
			System.out.println("Goldilocks discipline violated on variable " + this.getVariable().getName());
		}
		
		state.writeLockSet.put(x, new HashSet<Lock> ());
		state.writeLockSet.get(x).add(tLock);
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(GoldilocksState state, int verbosity) {
		Lock uLock = state.threadLocks.get(this.getTarget());
		
		for(Variable x: state.writeLockSet.keySet()){
			if(state.writeLockSet.get(x).contains(state.threadLocks.get(this.getThread()))){
				state.writeLockSet.get(x).add(uLock) ;
			}
		}
		for(Thread t: state.threadLocks.keySet()){
			Lock tLock = state.threadLocks.get(t);
			if(state.readLockSet.containsKey(tLock)){
				for(Variable x: state.readLockSet.get(tLock).keySet()){
					if(state.readLockSet.get(tLock).get(x).contains(state.threadLocks.get(this.getThread()))){
						state.readLockSet.get(tLock).get(x).add(uLock) ;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(GoldilocksState state, int verbosity) {
		Lock uLock = state.threadLocks.get(this.getTarget());
		
		for(Variable x: state.writeLockSet.keySet()){
			if(state.writeLockSet.get(x).contains(uLock)){
				state.writeLockSet.get(x).add(state.threadLocks.get(this.getThread())) ;
			}
		}
		for(Thread t: state.threadLocks.keySet()){
			Lock tLock = state.threadLocks.get(t);
			if(state.readLockSet.containsKey(tLock)){
				for(Variable x: state.readLockSet.get(tLock).keySet()){
					if(state.readLockSet.get(tLock).get(x).contains(uLock)){
						state.readLockSet.get(tLock).get(x).add(state.threadLocks.get(this.getThread())) ;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(GoldilocksState state, int verbosity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean HandleSubBegin(GoldilocksState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(GoldilocksState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
