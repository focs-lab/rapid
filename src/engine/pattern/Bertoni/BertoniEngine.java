package engine.pattern.Bertoni;

import engine.pattern.PatternEngine;
import parse.ParserType;

public class BertoniEngine extends PatternEngine<BertoniState, BertoniEvent> {

    public BertoniEngine(ParserType pType, String trace_folder, String patternFile) {
        super(pType, trace_folder, patternFile);
        handlerEvent = new BertoniEvent();
        state = new BertoniState(threadSet, pattern);
    }
}
