import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb.HBEngine;

public class HB {

	public HB() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		long startTimeAnalysis = System.currentTimeMillis();
		HBEngine engine = new HBEngine(options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		long stopTimeAnalysis = System.currentTimeMillis();
		long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
		System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
	}
}
