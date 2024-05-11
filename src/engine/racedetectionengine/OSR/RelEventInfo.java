package engine.racedetectionengine.OSR;

import util.vectorclock.VectorClock;

public class RelEventInfo extends EventInfo {

    public VectorClock TLClosure;

    public long auxId;

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
