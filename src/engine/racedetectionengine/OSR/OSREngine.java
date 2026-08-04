package engine.racedetectionengine.OSR;

import engine.racedetectionengine.OSR.POBuild.POBuildReverse;
import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;
import parse.std.ParseStandard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class OSREngine extends RaceDetectionEngine<OSRState, OSREvent> {

    public HashSet<String> orderedVariables;
    public HashMap<String, HashSet<String>> lockToThreadSet;


    public OSREngine(ParserType pType, String trace_dir) {
        super(pType);

        POBuildReverse po = new POBuildReverse(trace_dir);
        po.readTraceAndUpdateDS();
        int numThreads = po.numThreads;

        Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode = po.succToNode;
        Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode = po.succFromNode;
        ArrayList<Long>[] inThreadIdToAuxId = po.inThreadIdToAuxId;

        stdParser = new ParseStandard(trace_dir, false);
        this.state = new OSRState(succFromNode, succToNode, numThreads, inThreadIdToAuxId, 0);
        handlerEvent = new OSREvent();
        this.enablePrintStatus = false;
    }



    public OSRState getState(){
        return this.state;
    }


    public HashSet<Thread> getThreadSet() {
        return threadSet;
    }


    @Override
    protected boolean skipEvent(OSREvent handlerEvent) {
        return false;
    }


    @Override
    protected void postHandleEvent(OSREvent handlerEvent) {

    }
}
