import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.conflictserializability.thb.THBEngine;

public class THB {

	public THB() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		THBEngine engine = new THBEngine(options.parserType, options.path, options.verbosity);
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		engine.analyzeTrace(false);		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
