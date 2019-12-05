package engine.atomicity.conflictserializability.velodome_basic;

import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Transaction;

public class VelodromeEvent extends AtomicityEvent<VelodromeState> {

	//	@Override
	//	public boolean Handle(VCVelodromeState state) {
	//		return this.HandleSub(state);
	//	}

	@Override
	public void printRaceInfoLockType(VelodromeState state) {
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
	public void printRaceInfoTransactionType(VelodromeState state) {
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
	public void printRaceInfoAccessType(VelodromeState state) {
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
	public void printRaceInfoExtremeType(VelodromeState state) {
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
	public boolean HandleSubAcquire(VelodromeState state) {
		boolean violationDetected = false;
		
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);

		boolean outside = VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t));
		if(outside){
			HandleSubBegin(state);
		}
		Transaction n = state.threadToCurrentTransaction.get(t);
		Transaction u_l = state.lockToLastReleaseTransaction.get(l);
		violationDetected = state.specialUnionAndCheckCycle(u_l, n);
		if(outside){
			HandleSubEnd(state);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(VelodromeState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);

		boolean outside = VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t));
		if(outside){
			HandleSubBegin(state);
		}
		Transaction n = state.threadToCurrentTransaction.get(t);
		state.lockToLastReleaseTransaction.put(l, n);
		if(outside){
			HandleSubEnd(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(VelodromeState state) {
		
		boolean violationDetected = false;
		
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);

		boolean outside = VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t));
		if(outside){
			HandleSubBegin(state);
		}
		Transaction n = state.threadToCurrentTransaction.get(t);
		state.readVariableThreadToTransaction.get(v).put(t, n);
		Transaction w_v = state.writeVariableToTransaction.get(v);
		violationDetected = state.specialUnionAndCheckCycle(w_v, n);
		if(outside){
			HandleSubEnd(state);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(VelodromeState state) {

		boolean violationDetected = false;
		
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);

		boolean outside = VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t));
		if(outside){
			HandleSubBegin(state);
		}
		Transaction n = state.threadToCurrentTransaction.get(t);
		for(Thread tprime : state.threadToIndex.keySet()){
			Transaction r_v_tprime = state.readVariableThreadToTransaction.get(v).get(tprime);
			violationDetected = violationDetected || state.specialUnionAndCheckCycle(r_v_tprime, n);
		}
		Transaction w_v = state.writeVariableToTransaction.get(v);
		violationDetected = violationDetected || state.specialUnionAndCheckCycle(w_v, n);
		state.writeVariableToTransaction.put(v, n);
		if(outside){
			HandleSubEnd(state);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(VelodromeState state) {
		throw new IllegalArgumentException("Unhandled case: Fork unhandled."); 
	}

	@Override
	public boolean HandleSubJoin(VelodromeState state) {
		throw new IllegalArgumentException("Unhandled case: Join unhandled."); 
	}

	@Override
	public boolean HandleSubBegin(VelodromeState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		if(VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t))){
			Transaction n = state.getFreshTransaction(t);
			Transaction last_t = state.threadToLastOpTransaction.get(t);			
			violationDetected = state.specialUnionAndCheckCycle(last_t, n);
			state.threadToCurrentTransaction.put(t, n);
		}
		else{
			throw new IllegalArgumentException("Unhandled case: Expected bottom transaction at enter."); 
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubEnd(VelodromeState state) {
		Thread t = this.getThread();
		Transaction n = state.threadToCurrentTransaction.get(t);
		if(VelodromeState.isBottomTransaction(n)){
			throw new IllegalArgumentException("Unhandled case: Not expecting bottom transaction at exit.");
		}
		else{
			state.threadToCurrentTransaction.put(t, state.getBottomTransaction());
			state.threadToLastOpTransaction.put(t, n);			
		}
		return false;
	}

}
