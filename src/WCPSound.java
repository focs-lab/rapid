import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.wcp_sound.WCPEngine;
import parse.ParserType;

public class WCPSound {

	public WCPSound() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		WCPEngine engine = new WCPEngine(ParserType.STD, options.path, true, false, 0);
		engine.analyzeTrace(options.multipleRace, options.verbosity);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println(
					"Time for full analysis = " + timeAnalysis + " milliseconds");
		}

	}

	public static void analysis(String traceFile) {
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		WCPEngine engine = new WCPEngine(ParserType.STD, traceFile, true, false, 0);
		engine.analyzeTrace(true, 0);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
