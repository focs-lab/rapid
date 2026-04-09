import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.conflictserializability.atom_sanitizer.AtomSanitizerEngine;

public class AtomSanitizer {
	public AtomSanitizer() {
	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		AtomSanitizerEngine engine = new AtomSanitizerEngine(options.parserType, options.path, options.verbosity);

		boolean timeReporting = true;
		long startTimeAnalysis = 0L;
		if (timeReporting) {
			startTimeAnalysis = System.currentTimeMillis();
		}

		engine.analyzeTrace(options.multipleRace);

		if (timeReporting) {
			long stopTimeAnalysis = System.currentTimeMillis();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
