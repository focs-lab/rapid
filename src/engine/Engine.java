package engine;

import event.Event;
import parse.ParserType;
import parse.rv.RVParser;
import parse.std.ParseStandard;
import rapidutil.trace.Trace;

public abstract class Engine<E extends Event> {
	protected ParserType parserType;
	protected Trace trace; //CSV
	protected RVParser rvParser;//RV
	protected ParseStandard stdParser; //STD
	protected E handlerEvent;
	
	public Engine(ParserType pType){
		this.parserType = pType;
	}
	
	protected void initReaderStuff(String trace_folder){
		if(this.parserType.isRV()){
			initReaderStuffRV(trace_folder);
		}
		else if(this.parserType.isCSV()){
			initReaderStuffCSV(trace_folder);
		}
		else if(this.parserType.isSTD()){
			initReaderStuffSTD(trace_folder);
		}
	}
	
	protected abstract void initReaderStuffRV(String trace_folder);
	
	protected abstract void initReaderStuffCSV(String trace_file);
	
	protected abstract void initReaderStuffSTD(String trace_file);
}
