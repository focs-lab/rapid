package engine.racedetectionengine.grainSeqP.cmd;

import cmd.CmdOptions;

public class GrainSeqPCmdOptions extends CmdOptions {
	
	public int grainSize;
	public boolean doGrainSize;
	public boolean doGrainPattern;
	public boolean doLRU;
	public int LRUSize;
	public boolean doSubsumption;

	public GrainSeqPCmdOptions() {
		super();
		this.doGrainSize = false;
		this.doGrainPattern = false;
		this.doLRU = false;
		this.grainSize = 0;
		this.LRUSize = 0;
		this.doSubsumption = false;
	}

}
