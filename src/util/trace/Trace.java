package util.trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import event.Event;
import event.Thread;
/*
import wcp.event.EventType;
import wcp.thread.Thread;
import wcp.variable.Variable;
*/
public class Trace {

	private ArrayList<Event> eventList;

	public Trace() {
		eventList = new ArrayList<Event>();
	}

	public void addEvent(Event e) {
		eventList.add(e);
	}

	public ArrayList<Event> getEventList() {
		return eventList;
	}
	
	public int getSize(){
		return eventList.size();
	}
	
	public Event getEventAt(int index){
		Event e = null;
		if(index < eventList.size()){
			e = eventList.get(index);
		}
		return e;
	}

	public String toString() {
		return eventList.toString() + "\n";
	}

	public String toPrettyString() {
		String str = "{\n";
		for (int e_i = 0; e_i < eventList.size() - 1; e_i++) {
			Event e = eventList.get(e_i);
			str += Long.toString(e.getAuxId()) + "::" + e.toString();
			str += ",\n";
		}
		Event e = eventList.get(eventList.size() - 1);
		str += Long.toString(e.getAuxId()) + "::" + e.toString();
		str += "\n}\n";
		return str;
	}
	
	public void printPrototypeStyle(){
		for (int e_i = 0; e_i < eventList.size(); e_i++) {
			Event e = eventList.get(e_i);
			System.out.println(e.toPrototypeString());
		}	
	}
	
	public static Trace mergeTraceList(ArrayList<Trace> traceList) {

		Trace trace = new Trace();
		if (traceList.size() > 0) {
			// PriorityQueue is a sorted queue
			PriorityQueue<Event> q = new PriorityQueue<Event>(traceList.size(), new Comparator<Event>() {
				public int compare(Event a, Event b) {
					if (a.getAuxId() > b.getAuxId())
						return 1;
					else if (a.getAuxId() == b.getAuxId())
						return 0;
					else
						return -1;
				}
			});

			// The following container will map thread-names to indices in
			// traceList
			HashMap<Thread, Integer> threadMap = new HashMap<Thread, Integer>();
			int num_threads = traceList.size();
			int[] eventCounter = new int[num_threads]; // This is not
														// necessarily right.
														// The GID's were Long
			// add first node of each list to the queue
			for (int tr_i = 0; tr_i < traceList.size(); tr_i++) {
				Event e = traceList.get(tr_i).getEventList().get(0);
				q.add(e);
				eventCounter[tr_i] = 0;

				threadMap.put(e.getThread(), (Integer)tr_i);
			}

			while (q.size() > 0) {
				Event e = q.poll();
				trace.addEvent(e);
				int tr_i = threadMap.get(e.getThread());
				eventCounter[tr_i]++;
				// keep adding next element of each list
				if (eventCounter[tr_i] < traceList.get(tr_i).getEventList().size()) {
					q.add(traceList.get(tr_i).getEventList().get(eventCounter[tr_i]));
				}
			}
		}
		return trace;
	}
}
