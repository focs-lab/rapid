import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.fhb.FHBEngine;

public class FHB {

	public FHB() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		FHBEngine engine = new FHBEngine(options.parserType, options.path);
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}
		engine.analyzeTrace(options.multipleRace, options.verbosity);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
