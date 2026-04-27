import cmd.CmdOptions;
import cmd.GetOptions;
import engine.pattern.PatternTrack.VectorClockEngine;
import engine.pattern.ConfPreservingPrefix.PrefixEngine;

public class PatternLanguage {
    public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		String patternFile = args[args.length - 1];
		// VectorClockEngine engine = new VectorClockEngine(options.parserType, options.path, patternFile);
		PrefixEngine engine = new PrefixEngine(options.parserType, options.path, patternFile);
		boolean time_reporting = false;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		engine.analyzeTrace();
			
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
