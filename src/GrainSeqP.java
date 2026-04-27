import engine.racedetectionengine.grainSeqP.GrainSeqPEngine;
import engine.racedetectionengine.grainSeqP.cmd.*;
import event.Event;
import event.Lock;
import event.Thread;
import event.Variable;

import java.util.ArrayList;


public class GrainSeqP {
    public static void main(String[] args) {	
        GrainSeqPCmdOptions options = new GrainSeqPGetOptions(args).parse();

        class RunAnalyze extends java.lang.Thread {
            public GrainSeqPEngine prefixEngine;

            public RunAnalyze(int group) {
                Variable.variableCountTracker = 0;
                Thread.threadCountTracker = 0;
                Lock.lockCountTracker = 0;
                prefixEngine = new GrainSeqPEngine(options.parserType, options.path, options.doGrainPattern, options.doGrainSize, options.grainSize, options.doLRU, options.LRUSize, group);
                Event.eventCountTracker = (long)0;
            }

            @Override
            public void run() {
                prefixEngine.analyzeTrace();
            }
            
        }

        ArrayList<RunAnalyze> threads = new ArrayList<>();
        for(int i = 0; i < 1; i++) {
            threads.add(new RunAnalyze(i));
            threads.get(i).start();
        }
        for(int i = 0; i < 1; i++) {
            try {
                threads.get(i).join();
            }
            catch (Exception e) {
                System.out.println(e);
            } 
        }
        
	}
}
