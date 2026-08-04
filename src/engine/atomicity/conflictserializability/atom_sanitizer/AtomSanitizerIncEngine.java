package engine.atomicity.conflictserializability.atom_sanitizer;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class AtomSanitizerIncEngine extends AtomicityEngine<AtomSanitizerIncState, AtomSanitizerIncEvent> {
	public AtomSanitizerIncEngine(ParserType parserType, String traceFolder, int verbosity) {
		super(parserType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(traceFolder);
		this.state = new AtomSanitizerIncState(this.threadSet, verbosity);
		handlerEvent = new AtomSanitizerIncEvent();
	}

	@Override
	public void analyzeTrace(boolean multipleRace) {
		super.analyzeTrace(multipleRace);
		if (this.state.getBlame() != null) {
			System.out.println("Blame: " + this.state.getBlame().toString());
		}
	}

	@Override
	protected boolean skipEvent(AtomSanitizerIncEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(AtomSanitizerIncEvent handlerEvent) {
	}
}
