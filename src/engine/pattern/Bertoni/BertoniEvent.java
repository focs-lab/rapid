package engine.pattern.Bertoni;

import engine.pattern.PatternEvent;
import util.vectorclock.VectorClock;

public class BertoniEvent extends PatternEvent<BertoniState> {

    @Override
    public boolean Handle(BertoniState state) {
        super.Handle(state);
        state.history.get(thread).add(this.getTimeStamp(state));
        return state.computeNonTerm(this.locId, thread);
    }


    private void incrementCurrentThreadClock(BertoniState state, VectorClock vc) {
        int threadId = state.getThreadIndex(thread);
        vc.setClockIndex(threadId, vc.getClockIndex(threadId) + 1);
    }

    public boolean HandleSubAcquire(BertoniState state) {
        VectorClock L_l = state.getLockClock(lock);
        VectorClock C_t = state.getThreadClock(thread);
        C_t.updateWithMax(C_t, L_l);
        incrementCurrentThreadClock(state, C_t);
        return false;
    }

	public boolean HandleSubRelease(BertoniState state) {
        VectorClock L_l = state.getLockClock(lock);
        VectorClock C_t = state.getThreadClock(thread);
        incrementCurrentThreadClock(state, C_t);
        L_l.copyFrom(C_t);
        return false;
    }

	public boolean HandleSubRead(BertoniState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock W_x = state.getWriteClock(variable);
        VectorClock R_x = state.getReadClock(variable);
        C_t.updateWithMax(C_t, W_x);
        incrementCurrentThreadClock(state, C_t);
        R_x.updateWithMax(R_x, C_t);
        return false;
    }

	public boolean HandleSubWrite(BertoniState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock W_x = state.getWriteClock(variable);
        VectorClock R_x = state.getReadClock(variable);
        C_t.updateWithMax(C_t, W_x, R_x);
        incrementCurrentThreadClock(state, C_t);
        W_x.copyFrom(C_t);
        return false;
    }

	public boolean HandleSubFork(BertoniState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock C_u = state.getThreadClock(target);
        incrementCurrentThreadClock(state, C_t);
        C_u.copyFrom(C_t);
        return false;
    }

	public boolean HandleSubJoin(BertoniState state) {
        VectorClock C_t = state.getThreadClock(thread);
        VectorClock C_u = state.getThreadClock(target);
        C_t.updateWithMax(C_t, C_u);
        incrementCurrentThreadClock(state, C_t);
        return false;
    }

	public boolean HandleSubBegin(BertoniState state) {
        incrementCurrentThreadClock(state, state.getThreadClock(thread));
        return false;
    }

	public boolean HandleSubEnd(BertoniState state) {
        incrementCurrentThreadClock(state, state.getThreadClock(thread));
        return false;
    }

    private VectorClock getTimeStamp(BertoniState state) {
        return new VectorClock(state.getThreadClock(thread));
    }
}
