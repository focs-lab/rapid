package engine.racedetectionengine.grainSeqP.cmd;

import java.util.logging.Level;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import parse.ParserType;
import cmd.GetOptions;

public class GrainSeqPGetOptions extends GetOptions {

	public GrainSeqPGetOptions(String[] args) {
		super(args);
		options.addOption("lru", "lru", true, "Argument of Heuristic Optimizations");
		options.addOption("gs", "grainSize", true, "Heuristic Optimizations");
		options.addOption("gp", "grainPattern", false, "Heuristic Optimizations");
		options.addOption("sub", "subsumption", false, "Heuristic Optimizations");
	}

	public GrainSeqPCmdOptions parse() {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		GrainSeqPCmdOptions cmdOpt = new GrainSeqPCmdOptions();;

		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h"))
				help();

			if (cmd.hasOption("f")) {
				cmdOpt.parserType = ParserType.getType(cmd.getOptionValue("f")) ;   
			} 

			if (cmd.hasOption("s")) {
				cmdOpt.multipleRace = false;  
			}

			if (cmd.hasOption("v")) {
				try{
					cmdOpt.verbosity = Integer.parseInt(cmd.getOptionValue("v"));
					if(cmdOpt.verbosity < 0 || cmdOpt.verbosity > 3){
						log.log(Level.INFO, "Invalid verbosity level : " + cmdOpt.verbosity);
					}
				}
				catch (NumberFormatException nfe){
					log.log(Level.INFO, "Invalid verbosity option : " + cmd.getOptionValue("v"));
				}
			}

			if (cmd.hasOption("p")) {
				cmdOpt.path = cmd.getOptionValue("p") ;   
			}
			else {
				log.log(Level.INFO, "Missing path to file/folder");
				help();
			}
			
            if (cmd.hasOption("m")) {
                cmdOpt.excludeList = cmd.getOptionValue("m") ;   
            }

			if (cmd.hasOption("lru")) {
				cmdOpt.doLRU = true;
				cmdOpt.LRUSize = Integer.parseInt(cmd.getOptionValue("lru"));
			}

			if (cmd.hasOption("gs")) {
				cmdOpt.doGrainSize = true;
				cmdOpt.grainSize = Integer.parseInt(cmd.getOptionValue("gs"));
			}

			if(cmd.hasOption("gp")) {
				cmdOpt.doGrainPattern = true;
			}

			if(cmd.hasOption("sub")) {
				cmdOpt.doSubsumption = true;
			}

		} catch (ParseException e) {
			help();
		}

		return cmdOpt;
	}

	private void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("RAPID", options);
		System.exit(0);
	}

	public static void main(String[] args) {
		new GrainSeqPGetOptions(args).parse();
	}
}
