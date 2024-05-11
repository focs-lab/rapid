import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.PrintFilterEngine;
import parse.ParserType;

public class PrintSTDFilter {

	public PrintSTDFilter() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		PrintFilterEngine engine = new PrintFilterEngine(options.parserType, options.path);
		engine.analyzeTrace(ParserType.STD);		
	}
}
