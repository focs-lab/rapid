package engine.racedetectionengine.OSR;

import util.vectorclock.VectorClock;

public class AccessEventInfo extends EventInfo {
    public long auxId;

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
