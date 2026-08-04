package engine.racedetectionengine.OSR;


public class AcqEventInfo extends EventInfo {

    public RelEventInfo relEventInfo;

    public long auxId;


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
