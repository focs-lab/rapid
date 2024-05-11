import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb_epoch.HBEpochEngine;

public class HBEpoch {

	public HBEpoch() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis();
		}
		
		HBEpochEngine engine = new HBEpochEngine(options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}	
	}
}
