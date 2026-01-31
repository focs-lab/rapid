package engine.racedetectionengine.OSR_witness;


public class AcqEventInfo extends EventInfo {

    public RelEventInfo relEventInfo;
    public int lockId;


    public AcqEventInfo(){}

    @Override
    public String toString() {
        return "AcqEventInfo{" +
                "relEventInfo=" + relEventInfo +
                ", auxId=" + auxId +
                ", inThreadId=" + inThreadId +
                '}';
    }
}
