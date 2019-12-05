import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.conflictserializability.velodome_basic.VelodromeEngine;

public class VelodromeBasic {

	public VelodromeBasic() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = false;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		VelodromeEngine engine = new VelodromeEngine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(false);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
