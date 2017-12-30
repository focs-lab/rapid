package parse.csv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import event.Event;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import parse.util.CannotParseException;
import parse.util.EventInfo;
import util.trace.Trace;
import util.trace.TraceAndDataSets;


public class ParseCSV {

	public static HashMap<String, Thread> threadMap = new HashMap<String, Thread>();
	public static HashMap<String, Lock> lockMap = new HashMap<String, Lock>();
	public static HashMap<String, Variable> variableMap = new HashMap<String, Variable>();

	public static HashMap<Variable, HashSet<Thread>> threads_for_var = new HashMap<Variable, HashSet<Thread>>();
	public static HashMap<Variable, HashSet<Thread>> threads_for_readvar = new HashMap<Variable, HashSet<Thread>>();
	public static HashMap<Variable, HashSet<Thread>> threads_for_writevar = new HashMap<Variable, HashSet<Thread>>();

	public static HashSet<Variable> retainedVariables = new HashSet<Variable>();
	public static HashSet<Variable> discardedVariables = new HashSet<Variable>();

	public static HashSet<Thread> threadSet = new HashSet<Thread>();
	public static HashSet<Lock> lockSet = new HashSet<Lock>();

	public static void updateThreadsForReadVar(Variable v, Thread t) {
		if (!(threads_for_var.containsKey(v))) {
			threads_for_var.put(v, new HashSet<Thread>());
		}
		threads_for_var.get(v).add(t);

		if (!(threads_for_readvar.containsKey(v))) {
			threads_for_readvar.put(v, new HashSet<Thread>());
		}
		threads_for_readvar.get(v).add(t);
	}

	public static void updateThreadsForWriteVar(Variable v, Thread t) {
		if (!(threads_for_var.containsKey(v))) {
			threads_for_var.put(v, new HashSet<Thread>());
		}
		threads_for_var.get(v).add(t);

		if (!(threads_for_writevar.containsKey(v))) {
			threads_for_writevar.put(v, new HashSet<Thread>());
		}
		threads_for_writevar.get(v).add(t);
	}

	public static Event getEvent(EventType type, String tname, String aux, long GID, int LID) {
		if (!(threadMap.containsKey(tname))) {
			threadMap.put(tname, new Thread(tname));
			threadSet.add(threadMap.get(tname));
		}
		Thread t = threadMap.get(tname);
		Event e = null;

		if (type.isAcquire()) {
			String lname = aux;
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
				lockSet.add(lockMap.get(lname));
			}
			EventType tp = EventType.ACQUIRE;
			Lock l = lockMap.get(lname);
			e = new Event(GID, LID, tp, t, l, null, null);
		}

		else if (type.isRelease()) {
			String lname = aux;
			if (!(lockMap.containsKey(lname))) {
				// Trace is not well formed
				throw new IllegalArgumentException(
						"Trace is not well formed: Released lock " + lname + " is never encountered before ");
			}
			EventType tp = EventType.RELEASE;
			Lock l = lockMap.get(lname);
			e = new Event(GID, LID, tp, t, l, null, null);
		}

		else if (type.isRead()) {
			String vname = aux;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
				retainedVariables.add(variableMap.get(vname));
			}
			EventType tp = EventType.READ;
			Variable v = variableMap.get(vname);
			e = new Event(GID, LID, tp, t, null, v, null);
			updateThreadsForReadVar(v, t);
		}

		else if (type.isWrite()) {
			String vname = aux;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
				retainedVariables.add(variableMap.get(vname));
			}
			EventType tp = EventType.WRITE;
			Variable v = variableMap.get(vname);
			e = new Event(GID, LID, tp, t, null, v, null);
			updateThreadsForWriteVar(v, t);
		}

		else if (type.isFork()) {
			String target_name = aux;
			if (threadMap.containsKey(target_name)) {
				throw new IllegalArgumentException("Forked thread cannot exist before the fork event");
			}
			EventType tp = EventType.FORK;
			Thread target = new Thread(target_name);
			threadMap.put(target_name, target);
			threadSet.add(target);
			e = new Event(GID, LID, tp, t, null, null, target);
		}

		else if (type.isJoin()) {
			String target_name = aux;
			if (!(threadMap.containsKey(target_name))) {
				throw new IllegalArgumentException("Joined thread must be present before joining");
			}
			EventType tp = EventType.JOIN;
			Thread target = threadMap.get(target_name);
			e = new Event(GID, LID, tp, t, null, null, target);
		}

		else {
			throw new IllegalArgumentException("Illegal type of event " + type.toString());
		}

		return e;
	}
	
	//Without any annotations, or filtering of thread local vars
	public static Trace getRawTrace(String traceFile){
		Trace trace = new Trace();
		Parse parser = new Parse();
		String line = null;
		try {
			FileReader fileReader = new FileReader(traceFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			long cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				try{
					ArrayList<EventInfo> strList = parser.getInfoList(line);
					for(int i = 0; i < strList.size(); i ++){
						cnt ++;
						EventInfo str = strList.get(i);
						EventType type = str.type;
						String thread = str.thread;
						String aux = str.decor;
						Event e = getEvent(type,thread, aux, cnt, (int)cnt); //LocId is useless here, hence 0
						trace.addEvent(e);	
					}
				}
				catch (CannotParseException e){
					//It could be a comment 
				}
			}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + traceFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + traceFile + "'");
		}
		return trace;
	}
	
	public static TraceAndDataSets parse(boolean online, String traceFile) {
		Trace trace = getRawTrace(traceFile);
		if( online ){
			/** Do not annotate **/
			//trace.annotateOnline();
		}
		else {
			throw new IllegalArgumentException("Offline version not supported");
			//trace = removeThreadLocalEvents(trace);
			//trace.annotate();
		}
		//trace.printPrototypeStyle();
		return new TraceAndDataSets(trace, threadSet, lockSet, retainedVariables);
	}

	public static void main(String[] args) {
		//Parse.example();
		String traceFile = args[0];
		Trace trace = parse(true, traceFile).getTrace();
		trace.printPrototypeStyle();
	}

}
