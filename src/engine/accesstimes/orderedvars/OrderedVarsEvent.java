package engine.accesstimes.orderedvars;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.VectorClock;
import event.Thread;
import event.Variable;

public class OrderedVarsEvent extends RaceDetectionEvent<OrderedVarsState> {

	@Override
	public boolean Handle(OrderedVarsState state, int verbosity) {
		String t_name = this.getThread().getName();
		if(this.getType().isAccessType()){
			Variable v = this.getVariable();
			state.checkAndAddVariable(v);
			
			// Extra stuff that may already have been subsumed
			String v_name = v.getName();
			if(!state.variableToThreadSet.containsKey(v_name)){
				state.variableToThreadSet.put(v_name, new HashSet<String>());
			}
			state.variableToThreadSet.get(v_name).add(t_name);
			if(this.getType().isWrite()) {
				state.variablesWritten.add(v_name);
			}
		}
		if(this.getType().isLockType()){
			String l_name = this.getLock().getName();
			if(!state.lockToThreadSet.containsKey(l_name)){
				state.lockToThreadSet.put(l_name, new HashSet<String>());
			}
			state.lockToThreadSet.get(l_name).add(t_name);
		}
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(OrderedVarsState state, int verbosity) {	
	}

	@Override
	public void printRaceInfoAccessType(OrderedVarsState state, int verbosity) {		
	}

	@Override
	public void printRaceInfoExtremeType(OrderedVarsState state, int verbosity) {		
	}

	@Override
	public boolean HandleSubAcquire(OrderedVarsState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubRelease(OrderedVarsState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubRead(OrderedVarsState state, int verbosity) {
		Variable v = this.getVariable();
		
		
		Thread t = this.getThread();
		VectorClock C_t  =  state.getVectorClock(state.clockThread, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		
		String v_name = v.getName();
		if(state.orderedVariables.contains(v_name)) {
			boolean ordered = true;
			if(state.thread_lastWriteAccessForVariable.containsKey(v_name)) {
				int lastThreadIdToWrite_v = state.thread_lastWriteAccessForVariable.get(v_name);
				int clock_lastWrite = state.getClockIndex(LW_v, lastThreadIdToWrite_v);
				int t_view_of_lastWriteThread = state.getClockIndex(C_t, lastThreadIdToWrite_v);
				if(clock_lastWrite > t_view_of_lastWriteThread) {
					ordered = false;
				}
			}
			VectorClock lastVarRead_v  = state.getVectorClock(state.lastVariable_read, v);
			if(!ordered) {
				state.orderedVariables.remove(v_name);
				state.thread_lastWriteAccessForVariable.remove(v_name);
				lastVarRead_v = null;
				state.readVars.remove(v_name);
			}
			else {
				int tId = t.getId();
				int local_index = state.getClockIndex(C_t, tId);
				state.setClockIndex(lastVarRead_v, tId, local_index);
				state.readVars.add(v_name);
			}
		}
		
		C_t.updateWithMax(C_t, LW_v);
//		System.out.println(C_t);
		
		return false;
	}

	@Override
	public boolean HandleSubWrite(OrderedVarsState state, int verbosity) {
		Variable v = this.getVariable();
		
		
		Thread t = this.getThread();
		VectorClock C_t = state.getVectorClock(state.clockThread, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		
		String v_name = v.getName();
		if(state.orderedVariables.contains(v_name)) {
			boolean ordered = true;
			VectorClock lastVarRead_v  = state.getVectorClock(state.lastVariable_read, v);
			if(state.readVars.contains(v_name)) {
				if(!lastVarRead_v.isLessThanOrEqual(C_t)) {
					ordered = false;
				}
			}
			else {
				if(state.thread_lastWriteAccessForVariable.containsKey(v_name)) {
					int lastThreadIdToWrite_v = state.thread_lastWriteAccessForVariable.get(v_name);
					int clock_lastWrite = state.getClockIndex(LW_v, lastThreadIdToWrite_v);
					int t_view_of_lastWriteThread = state.getClockIndex(C_t, lastThreadIdToWrite_v);
					if(clock_lastWrite > t_view_of_lastWriteThread) {
						ordered = false;
					}
				}
			}
			if(!ordered) {
				state.orderedVariables.remove(v_name);
				state.thread_lastWriteAccessForVariable.remove(v_name);
				lastVarRead_v = null;
				state.readVars.remove(v_name);
			}
			else {
				state.thread_lastWriteAccessForVariable.put(v.getName(), t.getId());
				state.readVars.remove(v.getName());
			}
		}
		
//		System.out.println(C_t);
		LW_v.copyFrom(C_t);
		state.incClockThread(getThread());
		
		return false;
	}

	@Override
	public boolean HandleSubFork(OrderedVarsState state, int verbosity) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			VectorClock C_t = state.getVectorClock(state.clockThread, t);			
			VectorClock C_tc = state.getVectorClock(state.clockThread, tc);
			C_tc.copyFrom(C_t);
			state.setClockIndex(C_tc, tc.getId(), 1);
			state.incClockThread(getThread());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(OrderedVarsState state, int verbosity) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			VectorClock C_t = state.getVectorClock(state.clockThread, t);
			VectorClock C_tc = state.getVectorClock(state.clockThread, tc);
			C_t.updateWithMax(C_t, C_tc);
		}
		return false;
	}

	@Override
	public void printRaceInfoTransactionType(OrderedVarsState state, int verbosity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean HandleSubBegin(OrderedVarsState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(OrderedVarsState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

}
