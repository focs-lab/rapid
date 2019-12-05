package engine.atomicity.conflictserializability.thb;

import java.util.HashMap;
import java.util.HashSet;
import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClockOpt;

public class THBEvent extends AtomicityEvent<THBState> {

	@Override
	public void printRaceInfoLockType(THBState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoTransactionType(THBState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(THBState state) {
		if(this.getType().isAccessType()){
			if(state.verbosity == 1 || state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}	
		}		
	}

	@Override
	public void printRaceInfoExtremeType(THBState state) {
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public boolean HandleSubAcquire(THBState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);
		VectorClockOpt L_l = state.getVectorClock(state.clockLock, l);
		if(state.lastThreadToRelease.containsKey(l)) {
			if(!state.lastThreadToRelease.get(l).equals(t)) {
				violationDetected = state.checkAndGetClock(L_l, L_l, t);
			}
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(THBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);
		VectorClockOpt C_t = state.getVectorClock(state.clockThread, t);
		VectorClockOpt L_l = state.getVectorClock(state.clockLock, l);

		L_l.copyFrom( C_t);
		state.lastThreadToRelease.put(l, t);

		if(!state.transactionIsActive(t)){
			state.incClockThread(t);
		}
		
		return false;
	}

	@Override
	public boolean HandleSubRead(THBState state) {

		boolean violationDetected = false;	
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
		
		if(state.lastThreadToWrite.containsKey(v)) {
			Thread last_writer = state.lastThreadToWrite.get(v);
			if(!last_writer.equals(t)) {
				if(state.staleWrites.contains(v)) {
					VectorClockOpt C_last_writer = state.getVectorClock(state.clockThread, last_writer);
					violationDetected |= state.checkAndGetClock(C_last_writer, C_last_writer, t);
				}
				else {
					VectorClockOpt W_v = state.getVectorClock(state.clockWriteVariable, v);
					violationDetected |= state.checkAndGetClock(W_v, W_v, t);
				}
			}
		}
		
		state.staleReads.get(v).add(t);

		state.updateSetAtRead(t, v);
		
		if(!state.transactionIsActive(t)){
			state.incClockThread(t);
		}

		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(THBState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
					
		if(state.lastThreadToWrite.containsKey(v)) {
			Thread last_writer = state.lastThreadToWrite.get(v);
			if(!last_writer.equals(t)) {
				if(state.staleWrites.contains(v)) {
					VectorClockOpt C_last_writer = state.getVectorClock(state.clockThread, last_writer);
					violationDetected |= state.checkAndGetClock(C_last_writer, C_last_writer, t);
				}
				else {
					VectorClockOpt W_v = state.getVectorClock(state.clockWriteVariable, v);
					violationDetected |= state.checkAndGetClock(W_v, W_v, t);
				}
			}
		}
				
		state.getReadClocksFromStaleThreads(v);
				
		VectorClockOpt R_v = state.getVectorClock(state.clockReadVariable, v);
		VectorClockOpt chR_v = state.getVectorClock(state.clockReadVariableCheck, v);
		
		violationDetected |= state.checkAndGetClock(chR_v, R_v, t);
		
		state.staleWrites.add(v);
	
		state.lastThreadToWrite.put(v, t);

		state.updateSetAtWrite(t, v);
				
		if(!state.transactionIsActive(t)){
			state.incClockThread(t);
		}

		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();

			VectorClockOpt C_u = state.getVectorClock(state.clockThread, u);
			VectorClockOpt C_t = state.getVectorClock(state.clockThread, t);
			C_u.updateWithMax(C_u, C_t);
			
			if(!state.transactionIsActive(t)){
				state.incClockThread(t);
			}
			else {
				state.parentTransactionIsAlive.add(u);
				state.threadsForkedInActiveTransaction.get(t).add(u);
			}
			
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();
			VectorClockOpt C_u = state.getVectorClock(state.clockThread, u);
			return state.checkAndGetClock(C_u, C_u, t);
		}
		else return false;
	}

	@Override
	public boolean HandleSubBegin(THBState state) {
		Thread t = this.getThread();
		boolean violationDetected = false;

		if(!state.transactionIsActive(t)){
			state.incClockThread(t);
			VectorClockOpt C_t = state.getVectorClock(state.clockThread, t);				
			VectorClockOpt C_t_begin = state.getVectorClock(state.clockThreadBegin, t);
			C_t_begin.copyFrom(C_t);
		}
		// else Treat this as a no-op
		
		state.incrementNestingDepth(t);
		return violationDetected;
	}

	@Override
	public boolean HandleSubEnd(THBState state) {
		Thread t = this.getThread();
		boolean violationDetected = false;

		state.decrementNestingDepth(t);
		if(!state.transactionIsActive(t)) {
			if(state.hasIncomingEdge(t)) {
				violationDetected = state.handshakeAtEndEvent_Optimized(t);
//				state.incClockThread(t);
			}
			else {
				for(Variable v: state.updateSetThread_read.get(t)) {
					state.staleReads.get(v).remove(t);
				}
				state.updateSetThread_read.get(t).clear();
				
				for(Variable v: state.updateSetThread_write.get(t)) {
					if(state.lastThreadToWrite.containsKey(v)) {
						if(state.lastThreadToWrite.get(v).equals(t)) {
							state.staleWrites.remove(v);
							state.lastThreadToWrite.remove(v); // Treat this variable as a fresh variable
						}
					}	
				}
				state.updateSetThread_write.get(t).clear();
				
				HashSet<Lock> set_of_locks_written = new HashSet<Lock> ();
				for(HashMap.Entry<Lock, Thread> entry : state.lastThreadToRelease.entrySet()) {
					if(entry.getValue().equals(t)) set_of_locks_written.add(entry.getKey());
				}
				for(Lock l: set_of_locks_written) {
					state.lastThreadToRelease.remove(l);
				}
				
			}
			
			for(Thread u: state.threadsForkedInActiveTransaction.get(t)) {
				state.parentTransactionIsAlive.remove(u);
			}
			state.threadsForkedInActiveTransaction.get(t).clear();	
		}
		// else Treat this as no-op

		return violationDetected;
	}

}
