import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.conflictserializability.thb_basic.THBEngine;

public class THBBasic {

	public THBBasic() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		THBEngine engine = new THBEngine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(false);
				
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
