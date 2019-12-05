package engine.atomicity.conflictserializability.velodome;

import java.util.HashSet;

import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.Transaction;

public class VelodromeEvent extends AtomicityEvent<VelodromeState> {

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
		if(outside) {
			HashSet<Transaction> txn_set =  new HashSet<Transaction> ();
			txn_set.add(state.threadToLastOpTransaction.get(t));
			txn_set.add(state.lockToLastReleaseTransaction.get(l));
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					txn_set.add(state.parentTransaction.get(t));
				}
				state.startedThreads.add(t);
			}
			Pair<Transaction, Boolean> n_cycle = state.mergeAndCheckCycle(t, txn_set);
			state.threadToLastOpTransaction.put(t,  n_cycle.first);
			violationDetected = n_cycle.second;
		}
		else {
			Transaction n = state.threadToCurrentTransaction.get(t);
			Transaction u_l = state.lockToLastReleaseTransaction.get(l);
			violationDetected = state.specialUnionAndCheckCycle(u_l, n);
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					Transaction parent_tr = state.parentTransaction.get(t);
					violationDetected = violationDetected || state.specialUnionAndCheckCycle(parent_tr, n);
				}
				state.startedThreads.add(t);
			}
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
			state.lockToLastReleaseTransaction.put(l, state.threadToLastOpTransaction.get(t));
			// You cannot release without acquiring, so skipping the check for a parent
		}
		else {
			Transaction n = state.threadToCurrentTransaction.get(t);
			state.lockToLastReleaseTransaction.put(l, n);
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
			HashSet<Transaction> txn_set =  new HashSet<Transaction> ();
			txn_set.add(state.threadToLastOpTransaction.get(t));
			txn_set.add(state.writeVariableToTransaction.get(v));
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					txn_set.add(state.parentTransaction.get(t));
				}
				state.startedThreads.add(t);
			}
			Pair<Transaction, Boolean> n_cycle = state.mergeAndCheckCycle(t, txn_set);
			state.threadToLastOpTransaction.put(t,  n_cycle.first);
			state.readVariableThreadToTransaction.get(v).put(t, n_cycle.first);
			violationDetected = n_cycle.second;
		}
		else {
			Transaction n = state.threadToCurrentTransaction.get(t);
			state.readVariableThreadToTransaction.get(v).put(t, n);
			Transaction w_v = state.writeVariableToTransaction.get(v);
			violationDetected = state.specialUnionAndCheckCycle(w_v, n);
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					Transaction parent_tr = state.parentTransaction.get(t);
					violationDetected = violationDetected || state.specialUnionAndCheckCycle(parent_tr, n);
				}
				state.startedThreads.add(t);
			}
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
			HashSet<Transaction> txn_set =  new HashSet<Transaction> ();
			txn_set.add(state.threadToLastOpTransaction.get(t));
			txn_set.add(state.writeVariableToTransaction.get(v));
			for(Thread tprime: state.threadToIndex.keySet()) {
				txn_set.add(state.readVariableThreadToTransaction.get(v).get(tprime));
			}
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					txn_set.add(state.parentTransaction.get(t));
				}
				state.startedThreads.add(t);
			}
			Pair<Transaction, Boolean> n_cycle = state.mergeAndCheckCycle(t, txn_set);
			state.threadToLastOpTransaction.put(t,  n_cycle.first);
			state.writeVariableToTransaction.put(v, n_cycle.first);
			violationDetected = n_cycle.second;
		}
		else {
			Transaction n = state.threadToCurrentTransaction.get(t);
			for(Thread tprime : state.threadToIndex.keySet()){
				Transaction r_v_tprime = state.readVariableThreadToTransaction.get(v).get(tprime);
				violationDetected = violationDetected || state.specialUnionAndCheckCycle(r_v_tprime, n);
			}
			Transaction w_v = state.writeVariableToTransaction.get(v);
			violationDetected = violationDetected || state.specialUnionAndCheckCycle(w_v, n);
			state.writeVariableToTransaction.put(v, n);
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					Transaction parent_tr = state.parentTransaction.get(t);
					violationDetected = violationDetected || state.specialUnionAndCheckCycle(parent_tr, n);
				}
				state.startedThreads.add(t);
			}
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(VelodromeState state) {
		Thread t = this.getThread();
		Thread child = this.getTarget();
		
		Transaction n = null;
		if(!VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t))) 
			n = state.threadToCurrentTransaction.get(t);
		else if (!VelodromeState.isBottomTransaction(state.threadToLastOpTransaction.get(t))) n = state.threadToLastOpTransaction.get(t);
		
		if(n != null) state.parentTransaction.put(child, n);
		
		return false;
	}

	@Override
	public boolean HandleSubJoin(VelodromeState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Thread child_t = this.getTarget();
		Transaction child_tr = state.threadToLastOpTransaction.get(child_t);
		boolean outside = VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t));
		if(outside){
			HashSet<Transaction> txn_set =  new HashSet<Transaction> ();
			txn_set.add(state.threadToLastOpTransaction.get(t));
			txn_set.add(child_tr);
			Pair<Transaction, Boolean> n_cycle = state.mergeAndCheckCycle(t, txn_set);
			state.threadToLastOpTransaction.put(t,  n_cycle.first);
			violationDetected = n_cycle.second;
		}
		else {
			Transaction n = state.threadToCurrentTransaction.get(t);
			violationDetected = state.specialUnionAndCheckCycle(child_tr, n);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubBegin(VelodromeState state) {
		Thread t = this.getThread();

		int cur_depth = state.threadToNestingDepth.get(t);
		state.threadToNestingDepth.put(t,  cur_depth + 1);

		boolean violationDetected = false;

		if(VelodromeState.isBottomTransaction(state.threadToCurrentTransaction.get(t))){
			// This is the case when cur_depth = 0;
			Transaction n = state.getFreshTransaction(t);
			Transaction last_t = state.threadToLastOpTransaction.get(t);			
			violationDetected = state.specialUnionAndCheckCycle(last_t, n);
			state.threadToCurrentTransaction.put(t, n);
			// If this is the first event of the thread and there is a thread that forked this thread, add the previous transaction.
			if(!state.startedThreads.contains(t)) {
				if(state.parentTransaction.containsKey(t)) {
					Transaction parent_tr = state.parentTransaction.get(t);
					violationDetected = violationDetected || state.specialUnionAndCheckCycle(parent_tr, n);
				}
				state.startedThreads.add(t);
			}
		}
		else{
			// Treat this as a no-op
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubEnd(VelodromeState state) {
		Thread t = this.getThread();

		int cur_depth = state.threadToNestingDepth.get(t);
		state.threadToNestingDepth.put(t,  cur_depth - 1);
		if(cur_depth == 1) {
			Transaction n = state.threadToCurrentTransaction.get(t);
			state.threadToCurrentTransaction.put(t, state.getBottomTransaction());
			state.threadToLastOpTransaction.put(t, n);	
			state.garbageCollect(n);
		}
		else {
			// Treat this as no-op
		}
		return false;
	}

}
