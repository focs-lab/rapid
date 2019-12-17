package parse.std;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

import event.Event;
import event.Lock;
import event.Thread;
import event.Variable;
import parse.util.CannotParseException;
import parse.util.EventInfo;

public class ParseStandard {
	private HashMap<String, Thread> threadMap;
	private HashMap<String, Lock> lockMap;
	private HashMap<String, Variable> variableMap;
	int totThreads;
	BufferedReader bufferedReader;
	String line;
	Parse parser;
	EventInfo eInfo;
	long totEvents;

	public ParseStandard(String traceFile){
		threadMap = new HashMap<String, Thread>();
		lockMap = new HashMap<String, Lock>();
		variableMap = new HashMap<String, Variable>();
		totThreads = 0;
		totEvents = 0;

		bufferedReader = null;
		try{
			bufferedReader = new BufferedReader(new FileReader(traceFile));
		}
		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + traceFile + "'");
		}

		parser = new Parse();
		eInfo = new EventInfo();
		line = null;
	}
	
	public ParseStandard(String traceFile, boolean computeThreadSetAPriori){
		this(traceFile);
		if(computeThreadSetAPriori){
			Event e = new Event ();
			while(this.hasNext()){
				getNextEvent(e);
			}
			this.totEvents = 0; //Resetting totEvents is required to ensure correct AuxId
			try{
				bufferedReader = new BufferedReader(new FileReader(traceFile));
			}
			catch (FileNotFoundException ex) {
				System.out.println("Unable to open file '" + traceFile + "'");
			}
		}
	}
	
	public HashSet<Thread> getThreadSet(){
		return new HashSet<Thread> (this.threadMap.values());
	}

	public void eInfo2Event(Event e) {
		String tname = eInfo.thread;
		if (!(threadMap.containsKey(tname))) {
			threadMap.put(tname, new Thread(tname));
			totThreads = totThreads + 1;
		}
		Thread t = threadMap.get(tname);

		int LID = Integer.parseInt(eInfo.locId);
		String ename = "E" + Long.toString(totEvents);

		if (eInfo.type.isRead()) {
			String vname = eInfo.decor;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
			}
			Variable v = variableMap.get(vname);
			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, v, null);
		}

		else if (eInfo.type.isWrite()) {
			String vname = eInfo.decor;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
			}
			Variable v = variableMap.get(vname);

			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, v, null);
		}

		else if (eInfo.type.isAcquire()) {
			String lname = eInfo.decor;
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
			}
			Lock l = lockMap.get(lname);

			e.updateEvent(totEvents, LID, ename, eInfo.type, t, l, null, null);
		}

		else if (eInfo.type.isRelease()) {
			String lname = eInfo.decor;
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
			}
			Lock l = lockMap.get(lname);

			e.updateEvent(totEvents, LID, ename, eInfo.type, t, l, null, null);
		}

		else if (eInfo.type.isFork()) {
			String target_name = eInfo.decor;
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
			}
			Thread target = threadMap.get(target_name);

			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, null, target);
		}

		else if (eInfo.type.isJoin()) {
			String target_name = eInfo.decor;
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
			}
			Thread target = threadMap.get(target_name);

			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, null, target);
		}
		
		else if (eInfo.type.isBegin()) {
			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, null, null);
		}
		
		else if (eInfo.type.isEnd()) {
			e.updateEvent(totEvents, LID, ename, eInfo.type, t, null, null, null);
		}

		else {
			throw new IllegalArgumentException("Illegal type of event " + eInfo.type.toString());
		}

		totEvents = totEvents + 1;
	}

	public void getNextEvent(Event e){ //e is supposed to be over-written (deep copy) by the event-generated from the line read
		try {
			parser.getInfo(eInfo, line);
		} catch (CannotParseException ex) {
			System.err.println("Canot parse line -> " + line);
		}
		eInfo2Event(e);
	}

	public boolean hasNext(){
		try {
			line = bufferedReader.readLine() ;
		} catch (IOException ex) {
			System.err.println("Error reading buffered reader");
		}

		boolean endOfFile = (line == null);
		if(endOfFile){
			try {
				bufferedReader.close();
			} catch (IOException e) {
				System.err.println("Error closing buffered reader");
			}
		}
		return !endOfFile;
	}

	public int getTotalThreads(){
		return totThreads;
	}
	
	public static void demo(){
		String traceFile = "/Users/umang/Desktop/trace.txt";
		Event e = new Event();
		ParseStandard parser = new ParseStandard(traceFile);
		while(parser.hasNext()){
			parser.getNextEvent(e);
			System.out.println(e.toCompactString());
		}
	}
	
	public static void main(String args[]){
		demo();
	}

}
