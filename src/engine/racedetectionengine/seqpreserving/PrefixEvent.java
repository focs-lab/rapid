package engine.racedetectionengine.seqpreserving;

import event.Event;

public class PrefixEvent extends Event {

    public boolean Handle(State state) {
		return state.update(this);
	}   
}
