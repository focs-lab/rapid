package engine.racedetectionengine.grainSeqP;

import event.Event;

public class GrainSeqPEvent extends Event {
	public long eventCount = 0;
    public boolean Handle(State state) {
		eventCount += 1;
		return state.update(this);
	}   
}
