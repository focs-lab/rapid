package engine.racedetectionengine.hb;

import java.util.HashSet;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;
import parse.csv.ParseCSV;
import trace.TraceAndDataSets;
import parse.rv.RVParser;
import parse.std.ParseStandard;

public class HBEngine extends RaceDetectionEngine<HBState, HBEvent> {

	public HBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initReaderStuff(trace_folder);
		this.state = new HBState(this.threadSet);
		this.handlerEvent = new HBEvent();
	}
	
	@Override
	protected void initReaderStuffRV(String trace_folder){
		rvParser = new RVParser(trace_folder, this.threadSet);
	}

	@Override
	protected void initReaderStuffCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.threadSet = traceAndDataSets.getThreadSet();
		this.trace = traceAndDataSets.getTrace();
	}
	
	@Override
	protected void initReaderStuffSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file, true);
		threadSet = stdParser.getThreadSet();
	}

	public void analyzeTrace(boolean multipleRace, int verbosity){
		if(this.parserType.isRV()){
			analyzeTraceRV(multipleRace, verbosity);
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(multipleRace, verbosity);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(multipleRace, verbosity);
		}
	}
	
	protected boolean skipEvent(HBEvent handlerEvent){
		return false;
	}
	
	protected void postHandleEvent(HBEvent handlerEvent){}
}
