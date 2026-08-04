import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.OSR.OSREngine;
import engine.racedetectionengine.OSR.POBuild.POBuildReverse;
import engine.racedetectionengine.hb.HBEngine;
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
        CmdOptions options = new GetOptions(args).parse();
        long startTimeAnalysis = System.currentTimeMillis();

        OSREngine engine = new OSREngine(options.parserType, options.path);
        engine.analyzeTrace(options.multipleRace, options.verbosity);

        long stopTimeAnalysis = System.currentTimeMillis();
        long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
        double timeInSeconds = timeAnalysis * 1.0 / 1000;
        double timeInMin = timeAnalysis * 1.0 / 60000;
        ArrayList<Long> racyEvents = new ArrayList<>(engine.state.racyEvents);
        racyEvents.sort((a, b) -> Math.toIntExact(a - b));
        System.out.println("======================================================");
        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
        System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMin));
        System.out.println("Number of racy events: " + engine.state.racyEvents.size());
        System.out.println("Racy events: " + racyEvents);
        System.out.println("Number of racy locations: " + engine.state.racyLocations.size());
    }
}
