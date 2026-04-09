package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class AtomSanitizerEngine extends AtomicityEngine<AtomSanitizerState, AtomSanitizerEvent> {
	public AtomSanitizerEngine(ParserType parserType, String traceFolder, int verbosity) {
		super(parserType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(traceFolder);
		this.state = new AtomSanitizerState(this.threadSet, verbosity);
		handlerEvent = new AtomSanitizerEvent();
	}

	@Override
	protected boolean skipEvent(AtomSanitizerEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(AtomSanitizerEvent handlerEvent) {
	}
}
