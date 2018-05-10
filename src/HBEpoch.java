import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb_epoch.HBEpochEngine;

public class HBEpoch {

	public HBEpoch() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
//		if(options.multipleRace){
//			throw new IllegalArgumentException("The HB Epoch engine is supposed to run only until the first race is discovered.");
//		}
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		HBEpochEngine engine = new HBEpochEngine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}	
	}
}
