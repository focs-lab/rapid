package engine.racedetectionengine.OSR_witness;

import util.vectorclock.VectorClock;

public class AccessEventInfo extends EventInfo {
    public int varId;
    public long lastWrite;
    public int location; // program location

    public VectorClock prevTLC;
    public AccessEventInfo(){}

    @Override
    public String toString() {
        return "AccessEventInfo{" +
                "auxId=" + auxId +
                ", prevTLC=" + prevTLC +
                ", inThreadId=" + inThreadId +
                '}';
    }
}
