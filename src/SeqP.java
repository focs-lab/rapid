import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.seqpreserving.PrefixEngine;

public class SeqP {
    public static void main(String[] args) {	
        CmdOptions options = new GetOptions(args).parse();
       
        PrefixEngine engine = new PrefixEngine(options.parserType, options.path, true);
        engine.analyzeTrace(); 
    }
}
