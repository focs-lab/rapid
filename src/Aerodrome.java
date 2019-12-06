import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.conflictserializability.aerodrome.AerodromeEngine;

public class Aerodrome {

	public Aerodrome() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		AerodromeEngine engine = new AerodromeEngine(options.parserType, options.path, options.verbosity);
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
