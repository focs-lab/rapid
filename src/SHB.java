import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.shb.SHBEngine;

public class SHB {

	public SHB() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis();
		}

		SHBEngine engine = new SHBEngine(options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
