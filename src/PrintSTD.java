import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.PrintEngine;
import parse.ParserType;

public class PrintSTD {

	public PrintSTD() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		PrintEngine engine = new PrintEngine(options.parserType, options.path);
		engine.analyzeTrace(ParserType.STD);		
	}
}
