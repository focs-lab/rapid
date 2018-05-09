package engine.accesstimes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import engine.Engine;
import event.Event;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public class RefinedAccessTimesEngine extends Engine<Event> {

	public HashMap<String, Long> lockLast;
	public HashMap<String, Long> variableLast;
	public HashMap<String, HashMap<String, Long>> lockThreadLast;

	private HashMap<String, Stack<HashSet<String>>> threadStackReadVariables;
	private HashMap<String, Stack<HashSet<String>>> threadStackWriteVariables;
	private HashMap<String, HashMap<String, HashSet<String>>> readVariableToLockToThreadSet;
	private HashMap<String, HashMap<String, HashSet<String>>> writeVariableToLockToThreadSet;
	//public HashMap<String, HashSet<String>> lockReadVariables;
	//public HashMap<String, HashSet<String>> lockWriteVariables;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockReadVariableThreads;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockWriteVariableThreads;

	private long freshReadIndex;
	private long freshWriteIndex;
	private boolean newReadVariableSeen;
	private boolean newWriteVariableSeen;
	private Long indexOfNewlySeenReadVariable;
	private Long indexOfNewlySeenWriteVariable;
	private Long tempOldIndex;
	private Long tempNewIndex;
	public HashMap<String, Long> variableToReadEquivalenceClass;
	public HashMap<String, Long> variableToWriteEquivalenceClass;

	public HashMap<String, HashSet<String>> variableToThreadSet;
	public HashMap<String, HashSet<String>> lockToThreadSet;
	public HashSet<String> threadSet;
	public HashSet<String> variablesWritten;
	private HashMap<String, String> parentMap;

	//private HashMap<String, Stack<String>> threadToActiveLocks;
	//public HashMap<String, HashSet<String>> variableToAccessdLocks;

	public RefinedAccessTimesEngine(ParserType pType, String trace_folder) {
		super(pType);
		initializeReader(trace_folder);

		handlerEvent = new Event();
		lockLast = new HashMap<String, Long> () ;
		variableLast = new HashMap<String, Long> () ;
		lockThreadLast = new HashMap<String, HashMap<String, Long> >();

		threadStackReadVariables = new HashMap<String, Stack<HashSet<String>>>();
		threadStackWriteVariables = new HashMap<String, Stack<HashSet<String>>>();
		readVariableToLockToThreadSet = new HashMap<String, HashMap<String, HashSet<String>>>();
		writeVariableToLockToThreadSet = new HashMap<String, HashMap<String, HashSet<String>>>();
		//lockReadVariables = new HashMap<String, HashSet<String>>();
		//lockWriteVariables = new HashMap<String, HashSet<String>>();
		existsLockReadVariableThreads = new HashMap<String, HashMap<String, HashSet<String>>>();
		existsLockWriteVariableThreads = new HashMap<String, HashMap<String, HashSet<String>>>();

		//threadToActiveLocks = new HashMap<String, Stack<String>> ();
		//variableToAccessdLocks = new HashMap<String, HashSet<String>>();

		freshReadIndex = (long) 0;
		freshWriteIndex = (long) 0;
		newReadVariableSeen = false;
		newWriteVariableSeen = false;
		indexOfNewlySeenReadVariable = (long) 0;
		indexOfNewlySeenWriteVariable = (long) 0;
		tempOldIndex = (long) 0;
		tempNewIndex = (long) 0;
		variableToReadEquivalenceClass = new HashMap<String, Long>();
		variableToWriteEquivalenceClass = new HashMap<String, Long>();

		variableToThreadSet = new HashMap<String, HashSet<String>>();
		lockToThreadSet = new HashMap<String, HashSet<String>>();
		threadSet = new HashSet<String> ();
		variablesWritten = new HashSet<String> ();

		parentMap = new HashMap<String, String> ();
	}

	private void deleteObseleteThreads(){
		HashMap<String, Integer> threadAccessCount = new HashMap<String, Integer> ();
		for(String tstr : threadSet){
			threadAccessCount.put(tstr, 0);
		}

		for(String vstr : variableToThreadSet.keySet()){
			if(variablesWritten.contains(vstr)){
				HashSet<String> threads = variableToThreadSet.get(vstr);
				for(String tstr : threads){
					int cnt = threadAccessCount.get(tstr);
					threadAccessCount.put(tstr, cnt+1);
				}	
			}
			else{
//				System.out.println("Read only " + vstr);
//				System.out.println("Threads " + variableToThreadSet.get(vstr));
			}
		}

		for(String lstr : lockToThreadSet.keySet()){
			HashSet<String> threads = lockToThreadSet.get(lstr);
			for(String tstr : threads){
				int cnt = threadAccessCount.get(tstr);
				threadAccessCount.put(tstr, cnt+1);
			}
		}

		for(String tstr : parentMap.keySet()){
			String p_tstr = parentMap.get(tstr);
			int cnt = threadAccessCount.get(p_tstr);
			threadAccessCount.put(p_tstr, cnt+1);
		}

		boolean change = true;
		while(change){
			change = false;
			HashSet<String> removedToBe = new HashSet<String> ();
			for(String tstr : threadSet){
				if(threadAccessCount.get(tstr) == 0){
					change = true;
					removedToBe.add(tstr);
					String p_str = parentMap.get(tstr);
					int new_parent_cnt = threadAccessCount.get(p_str) - 1;
					threadAccessCount.put(p_str, new_parent_cnt);
				}
			}

			for(String rem : removedToBe){
				threadSet.remove(rem);
			}
		}
	}

	public void computeLastAccessTimes(){
		if(this.parserType.isRV()){
			computeLastAccessTimesRV();
		}
		else if(this.parserType.isCSV()){
			computeLastAccessTimesCSV();
		}
		else if(this.parserType.isSTD()){
			computeLastAccessTimesSTD();
		}
		else if(this.parserType.isRR()){
			computeLastAccessTimesRR();
		}
		deleteObseleteThreads();
	}

	public void computeLastAccessTimesRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				processEvent();
			}
		}
	}

	public void computeLastAccessTimesCSV() {
		for(int eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			processEvent();
		}
	}
	
	public void computeLastAccessTimesSTD() {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			processEvent();
		}
	}
	
	public void computeLastAccessTimesRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			processEvent();
		}
	}

	private void processEvent(){
		threadSet.add(handlerEvent.getThread().getName());

		long eventIndex = handlerEvent.getAuxId();

		if(!threadStackReadVariables.containsKey( handlerEvent.getThread().getName() )){
			threadStackReadVariables.put(handlerEvent.getThread().getName(), new Stack<HashSet<String>>());
			threadStackWriteVariables.put(handlerEvent.getThread().getName(), new Stack<HashSet<String>>());
			/*
			threadToActiveLocks.put(handlerEvent.getThread().getName(), new Stack<String> ());
			 */
		}

		if(handlerEvent.getType().isLockType()){
			if(!lockToThreadSet.containsKey(handlerEvent.getLock().getName())){
				lockToThreadSet.put(handlerEvent.getLock().getName(), new HashSet<String>());
			}
			lockToThreadSet.get(handlerEvent.getLock().getName()).add(handlerEvent.getThread().getName());

			lockLast.put(handlerEvent.getLock().getName(), eventIndex);

			if(! (lockThreadLast.containsKey(handlerEvent.getLock().getName()))){
				lockThreadLast.put(handlerEvent.getLock().getName(), new HashMap<String, Long>());
				/*
				lockReadVariables.put(handlerEvent.getLock().getName(), new HashSet<String>());
				lockWriteVariables.put(handlerEvent.getLock().getName(), new HashSet<String>());
				 */
				existsLockReadVariableThreads.put(handlerEvent.getLock().getName(), new HashMap<String, HashSet<String>>());
				existsLockWriteVariableThreads.put(handlerEvent.getLock().getName(), new HashMap<String, HashSet<String>>());
			}
			lockThreadLast.get(handlerEvent.getLock().getName()).put(handlerEvent.getThread().getName(), eventIndex);

			if(handlerEvent.getType().isAcquire()){
				threadStackReadVariables.get(handlerEvent.getThread().getName()).push(new HashSet<String>());
				threadStackWriteVariables.get(handlerEvent.getThread().getName()).push(new HashSet<String>());

				/*
				threadToActiveLocks.get(handlerEvent.getThread().getName()).push(handlerEvent.getLock().getName());
				 */
			}
			else if(handlerEvent.getType().isRelease()){
				HashSet<String> readVarSet = threadStackReadVariables.get(handlerEvent.getThread().getName()).pop();
				HashSet<String> writeVarSet = threadStackWriteVariables.get(handlerEvent.getThread().getName()).pop();
				if (!(threadStackReadVariables.get(handlerEvent.getThread().getName()).isEmpty())){
					threadStackReadVariables.get(handlerEvent.getThread().getName()).peek().addAll(readVarSet);
					threadStackWriteVariables.get(handlerEvent.getThread().getName()).peek().addAll(writeVarSet);
				}
				/*
				lockReadVariables.get(handlerEvent.getLock().getName()).addAll(readVarSet);
				lockWriteVariables.get(handlerEvent.getLock().getName()).addAll(writeVarSet);
				 */

				for(String rVar: readVarSet){
					if(!readVariableToLockToThreadSet.containsKey(rVar)){
						readVariableToLockToThreadSet.put(rVar, new HashMap<String, HashSet<String>>());
					}
					if(!readVariableToLockToThreadSet.get(rVar).containsKey(handlerEvent.getLock().getName())){
						readVariableToLockToThreadSet.get(rVar).put(handlerEvent.getLock().getName(), new HashSet<String>());
					}
					readVariableToLockToThreadSet.get(rVar).get(handlerEvent.getLock().getName()).add(handlerEvent.getThread().getName());

					//Check if there was a critical section before this on the same lock in some other thread in which rVar was written. If so, add rVar to lockWriteVariables
					if(writeVariableToLockToThreadSet.containsKey(rVar)){
						if(writeVariableToLockToThreadSet.get(rVar).containsKey(handlerEvent.getLock().getName())){
							boolean anyOtherThread = false;
							if(writeVariableToLockToThreadSet.get(rVar).get(handlerEvent.getLock().getName()).contains(handlerEvent.getThread().getName())) {
								if( writeVariableToLockToThreadSet.get(rVar).get(handlerEvent.getLock().getName()).size() > 1 ){
									anyOtherThread = true;
								}
							}
							else{
								if( writeVariableToLockToThreadSet.get(rVar).get(handlerEvent.getLock().getName()).size() > 0 ){
									anyOtherThread = true;
								}
							}
							if (anyOtherThread){
								if(! existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).containsKey(rVar)){
									existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).put(rVar, new HashSet<String>());
								}
								existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).get(rVar).add(handlerEvent.getThread().getName());
							}
						}
					}

				}
				for(String wVar: writeVarSet){
					if(!writeVariableToLockToThreadSet.containsKey(wVar)){
						writeVariableToLockToThreadSet.put(wVar, new HashMap<String, HashSet<String>>());
					}
					if(!writeVariableToLockToThreadSet.get(wVar).containsKey(handlerEvent.getLock().getName())){
						writeVariableToLockToThreadSet.get(wVar).put(handlerEvent.getLock().getName(), new HashSet<String>());
					}
					writeVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).add(handlerEvent.getThread().getName());

					//Check if there was a critical section before this on the same lock in some other thread in which wVar was read. If so, add wVar to lockReadVariables
					if(readVariableToLockToThreadSet.containsKey(wVar)){
						if(readVariableToLockToThreadSet.get(wVar).containsKey(handlerEvent.getLock().getName())){
							boolean anyOtherThread = false;
							if(readVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).contains(handlerEvent.getThread().getName())) {
								if( readVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).size() > 1 ){
									anyOtherThread = true;
								}
							}
							else{
								if( readVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).size() > 0 ){
									anyOtherThread = true;
								}
							}
							if (anyOtherThread){

								if(! existsLockReadVariableThreads.get(handlerEvent.getLock().getName()).containsKey(wVar)){
									existsLockReadVariableThreads.get(handlerEvent.getLock().getName()).put(wVar, new HashSet<String>());
								}
								existsLockReadVariableThreads.get(handlerEvent.getLock().getName()).get(wVar).add(handlerEvent.getThread().getName());
							}
						}
					}


					//Check if there was a critical section before this on the same lock in some other thread in which wVar was written. If so, add wVar to lockWriteVariables
					if(writeVariableToLockToThreadSet.containsKey(wVar)){
						if(writeVariableToLockToThreadSet.get(wVar).containsKey(handlerEvent.getLock().getName())){
							boolean anyOtherThread = false;
							if(writeVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).contains(handlerEvent.getThread().getName())) {
								if( writeVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).size() > 1 ){
									anyOtherThread = true;
								}
							}
							else{
								if( writeVariableToLockToThreadSet.get(wVar).get(handlerEvent.getLock().getName()).size() > 0 ){
									anyOtherThread = true;
								}
							}
							if (anyOtherThread){

								if(! existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).containsKey(wVar)){
									existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).put(wVar, new HashSet<String>());
								}
								existsLockWriteVariableThreads.get(handlerEvent.getLock().getName()).get(wVar).add(handlerEvent.getThread().getName());
							}
						}
					}

				}

				HashMap<Long, Long> readOldIndexToNewIndex = new HashMap<Long, Long>(); 
				newReadVariableSeen = false;
				for (String rVar : readVarSet){
					if(!variableToReadEquivalenceClass.containsKey(rVar)){
						if(! newReadVariableSeen){
							indexOfNewlySeenReadVariable = freshReadIndex;
							freshReadIndex = freshReadIndex + 1;
							newReadVariableSeen = true;
						}
						variableToReadEquivalenceClass.put(rVar, indexOfNewlySeenReadVariable);
					}
					else{
						if(!readOldIndexToNewIndex.containsKey(variableToReadEquivalenceClass.get(rVar))){
							readOldIndexToNewIndex.put(variableToReadEquivalenceClass.get(rVar), freshReadIndex);
							freshReadIndex = freshReadIndex + 1;
						}
						tempOldIndex = variableToReadEquivalenceClass.get(rVar);
						tempNewIndex = readOldIndexToNewIndex.get(tempOldIndex);
						variableToReadEquivalenceClass.put(rVar, tempNewIndex);
					}
				}

				HashMap<Long, Long> writeOldIndexToNewIndex = new HashMap<Long, Long>(); 
				newWriteVariableSeen = false;
				for (String wVar : writeVarSet){
					if(!variableToWriteEquivalenceClass.containsKey(wVar)){
						if(! newWriteVariableSeen){
							indexOfNewlySeenWriteVariable = freshWriteIndex;
							freshWriteIndex = freshWriteIndex + 1;
							newWriteVariableSeen = true;
						}
						variableToWriteEquivalenceClass.put(wVar, indexOfNewlySeenWriteVariable);
					}
					else{
						if(!writeOldIndexToNewIndex.containsKey(variableToWriteEquivalenceClass.get(wVar))){
							writeOldIndexToNewIndex.put(variableToWriteEquivalenceClass.get(wVar), freshWriteIndex);
							freshWriteIndex = freshWriteIndex + 1;
						}
						tempOldIndex = variableToWriteEquivalenceClass.get(wVar);
						tempNewIndex = writeOldIndexToNewIndex.get(tempOldIndex);
						variableToWriteEquivalenceClass.put(wVar, tempNewIndex);
					}
				}

				/*
				threadToActiveLocks.get(handlerEvent.getThread().getName()).pop();
				for (String rVar : readVarSet){
					variableToAccessdLocks.get(rVar).add(handlerEvent.getLock().getName());
				}
				for (String wVar : writeVarSet){
					variableToAccessdLocks.get(wVar).add(handlerEvent.getLock().getName());
				}
				 */
			}
		}

		if(handlerEvent.getType().isAccessType()){
			if(!variableToThreadSet.containsKey(handlerEvent.getVariable().getName())){
				variableToThreadSet.put(handlerEvent.getVariable().getName(), new HashSet<String>());
			}
			variableToThreadSet.get(handlerEvent.getVariable().getName()).add(handlerEvent.getThread().getName());

			variableLast.put(handlerEvent.getVariable().getName(), eventIndex);
			if(handlerEvent.getType().isRead()){
				if (!(threadStackReadVariables.get(handlerEvent.getThread().getName()).isEmpty())){
					threadStackReadVariables.get(handlerEvent.getThread().getName()).peek().add(handlerEvent.getVariable().getName());
				}
			}
			else if(handlerEvent.getType().isWrite()){
				if (!(threadStackWriteVariables.get(handlerEvent.getThread().getName()).isEmpty())){
					threadStackWriteVariables.get(handlerEvent.getThread().getName()).peek().add(handlerEvent.getVariable().getName());
				}
				variablesWritten.add(handlerEvent.getVariable().getName());
			}
		}

		if(handlerEvent.getType().isExtremeType()){
			parentMap.put(handlerEvent.getTarget().getName(), handlerEvent.getThread().getName());
		}
	}


	@Override
	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, null);
	}

	@Override
	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
	}

	@Override
	protected void initializeReaderSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file);
	}
	
	@Override
	protected void initializeReaderRR(String trace_file) {
		rrParser = new ParseRoadRunner(trace_file);
	}
}
