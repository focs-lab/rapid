package engine.racedetectionengine.OSR_witness;

import util.vectorclock.VectorClock;

public class RelEventInfo extends EventInfo {

    public VectorClock TLClosure;
    public int lockId;

    public RelEventInfo(){}

    @Override
    public String toString() {
        return "RelEventInfo{" +
                ", TLClosure=" + TLClosure +
                ", auxId=" + auxId +
                ", inThreadId=" + inThreadId +
                '}';
    }
}
