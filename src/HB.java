import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb.HBEngine;

public class HB {

	public HB() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		long startTimeAnalysis = System.nanoTime();
		HBEngine engine = new HBEngine(options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		long stopTimeAnalysis = System.nanoTime();
		long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
		System.out.println("Time for analysis = " + timeAnalysis + " nanoseconds");
	}
}
