package engine.pattern.PatternTrack;

import engine.pattern.PatternEngine;
import parse.ParserType;

public class VectorClockEngine extends PatternEngine<VectorClockState, VectorClockEvent> {

    public VectorClockEngine(ParserType pType, String trace_folder, String patternFile) {
        super(pType, trace_folder, patternFile);
        handlerEvent = new VectorClockEvent();
        state = new VectorClockState(threadSet, pattern);
    }

}
