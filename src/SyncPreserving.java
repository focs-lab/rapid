import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.syncpreserving.SyncPreservingRaceOfflineEngine;

public class SyncPreserving {

	public SyncPreserving() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		SyncPreservingRaceOfflineEngine engine = new SyncPreservingRaceOfflineEngine(
				options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
