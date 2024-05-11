import engine.racedetectionengine.OSR.OSREngine;
import engine.racedetectionengine.OSR.POBuild.POBuildReverse;
import event.Event;
import event.Lock;
import event.Thread;
import event.Variable;
import parse.ParserType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OSR {
    public static void main(String[] args) throws IOException {
        String traceDir = args[0];
        analysis(traceDir);
    }

    public static void analysis(String trace_dir) throws IOException {
        boolean time_reporting = true;
        long startTimeAnalysis = 0;
        if (time_reporting) {
            startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
        }

        POBuildReverse po = new POBuildReverse(trace_dir);
        po.readTraceAndUpdateDS();
        int numThreads = po.numThreads;
//        po.printArray();

        Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode = po.succToNode;
        Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode = po.succFromNode;
        ArrayList<Long>[] inThreadIdToAuxId = po.inThreadIdToAuxId;

        long mid = System.currentTimeMillis();
        long firstPhase = (mid - startTimeAnalysis) / 1000;

        OSREngine engine = new OSREngine(ParserType.STD, trace_dir, numThreads, succFromNode, succToNode, inThreadIdToAuxId);
        succToNode = null;
        succFromNode = null;
        engine.analyzeTrace(true, 0);

        Thread.threadCountTracker = 0;
        Lock.lockCountTracker = 0;
        Variable.variableCountTracker = 0;
        Event.eventCountTracker = 0L;

        long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
        long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
        double timeInSeconds = timeAnalysis * 1.0 / 1000;
        double timeInMin = timeAnalysis * 1.0 / 60000;
        ArrayList<Long> racyEvents = new ArrayList<>(engine.state.racyEvents);
        racyEvents.sort((a, b) -> Math.toIntExact(a - b));
        System.out.println("======================================================");
        System.out.println("Time for preprocessing phase = " + firstPhase + " seconds");
        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
        System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMin));
        System.out.println("Number of racy events: " + engine.state.racyEvents.size());
        System.out.println("Racy events: " + racyEvents);
        System.out.println("Number of racy locations: " + engine.state.racyLocations.size());
//        System.out.println("Race Distances = " + engine.state.raceDistances);
        System.out.println(trace_dir + "                              OSR");

    }
}
