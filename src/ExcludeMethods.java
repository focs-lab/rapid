import cmd.CmdOptions;
import cmd.GetOptions;
import event.Event;
import parse.rr.ParseRoadRunner;

public class ExcludeMethods {

	public ExcludeMethods() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		String traceFile = options.path;
		String excludeFile = options.excludeList;
		Event e = new Event();
		ParseRoadRunner parser;
		if(!(excludeFile == null)) {
			parser = new ParseRoadRunner(traceFile, excludeFile);
		}
		else {
			parser = new ParseRoadRunner(traceFile);
		}
		while(parser.checkAndGetNext(e)){
			System.out.println(e.toStandardFormat());
		}
	}
}
