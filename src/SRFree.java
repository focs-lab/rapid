import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetection.syncreversalfree.distance.SRFreeOfflineEngine;

public class SRFree {

	public SRFree() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		SRFreeOfflineEngine engine = new SRFreeOfflineEngine(options.parserType, options.path);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
