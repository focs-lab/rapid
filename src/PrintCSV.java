import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.PrintEngine;
import parse.ParserType;

public class PrintCSV {

	public PrintCSV() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		PrintEngine engine = new PrintEngine(options.parserType, options.path);
		engine.analyzeTrace(ParserType.CSV);
	}
}
