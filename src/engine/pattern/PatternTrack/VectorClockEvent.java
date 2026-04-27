package engine.pattern.PatternTrack;

import engine.pattern.PatternEvent;
import util.vectorclock.VectorClock;


public class VectorClockEvent extends PatternEvent<VectorClockState> {
    
    @Override
    public boolean Handle(VectorClockState state) {
        super.Handle(state);
        return state.extendWitness(this.locId, this.thread, this.getTimeStamp(state));
    }

    private void incrementCurrentThreadClock(VectorClockState state, VectorClock vc) {
        int threadId = state.getThreadIndex(thread);
        vc.setClockIndex(threadId, vc.getClockIndex(threadId) + 1);
    }

    public boolean HandleSubAcquire(VectorClockState state) {
        VectorClock L_l = state.getLockClock(lock);
        VectorClock C_t = state.getThreadClock(thread);
        C_t.updateWithMax(C_t, L_l);
        incrementCurrentThreadClock(state, C_t);
        return false;
    }

	public boolean HandleSubRelease(VectorClockState state) {
        VectorClock L_l = state.getLockClock(lock);
        VectorClock C_t = state.getThreadClock(thread);
        incrementCurrentThreadClock(state, C_t);
        L_l.copyFrom(C_t);
        return false;
    }

	public boolean HandleSubRead(VectorClockState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock W_x = state.getWriteClock(variable);
        VectorClock R_x = state.getReadClock(variable);
        C_t.updateWithMax(C_t, W_x);
        incrementCurrentThreadClock(state, C_t);
        R_x.updateWithMax(R_x, C_t);
        return false;
    }

	public boolean HandleSubWrite(VectorClockState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock W_x = state.getWriteClock(variable);
        VectorClock R_x = state.getReadClock(variable);
        C_t.updateWithMax(C_t, W_x, R_x);
        incrementCurrentThreadClock(state, C_t);
        W_x.copyFrom(C_t);
        return false;
    }

	public boolean HandleSubFork(VectorClockState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock C_u = state.getThreadClock(target);
        incrementCurrentThreadClock(state, C_t);
        C_u.copyFrom(C_t);
        C_u.setClockIndex(state.getThreadIndex(target), 1);
        return false;
    }

	public boolean HandleSubJoin(VectorClockState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock C_u = state.getThreadClock(target);
        C_t.updateWithMax(C_t, C_u);
        incrementCurrentThreadClock(state, C_t);
        return false;
    }

	public boolean HandleSubBegin(VectorClockState state) {
        incrementCurrentThreadClock(state, state.getThreadClock(thread));
        return false;
    }

	public boolean HandleSubEnd(VectorClockState state) {
        incrementCurrentThreadClock(state, state.getThreadClock(thread));
        return false;
    }

    private VectorClock getTimeStamp(VectorClockState state) {
        return new VectorClock(state.getThreadClock(thread));
    }
}


