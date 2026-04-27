package engine.racedetectionengine.seqpreserving.preprocess;

import event.Event;

public class PreprocessEvent extends Event {

    public boolean Handle(State state) {
		return state.update(this);
	}   
}
