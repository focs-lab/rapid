package engine.racedetectionengine.OSR.POBuild;

import util.Triplet;
import util.vectorclock.VectorClock;

import java.util.*;

// This class exposes the method for abstract graph building

public class PartialOrder {
	public int width; // number of threads
	public List<Map<Integer, RangeMinimaQuery>> successors;

	public PartialOrder(Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode,
						Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode, int numThreads) {
		this.width = numThreads;
		this.successors = new ArrayList<>();

		for(int i=0; i< this.width; i++) {
			this.successors.add(new HashMap<>());
			for(int j=0; j<this.width; j++) {
				if(i!=j) {
					ArrayList<Integer> fromNodes = succFromNode.get(i).get(j);
					ArrayList<Integer> toNodes = succToNode.get(i).get(j);
					this.successors.get(i).put(j, new RangeMinimaQuery(fromNodes, toNodes));
					toNodes = null;
					succFromNode.get(i).remove(j);
					succToNode.get(i).remove(j);
				}
			}
			succFromNode.remove(i);
			succToNode.remove(i);
		}
	}
	
	public void pause() {
		try {System.in.read();} catch(Exception e) {}
	}
	

	// Given an event e1 and osr set, return the earliest events e2 in every thread,
	// s.t. there is a forward path from e1 to e2
	public VectorClock queryForEvent(Triplet<Integer, Integer, Long> event, VectorClock osr,
									 ArrayList<Long>[] inThreadIdToAuxId){
		// event = <threadId, inThreadId, auxId>
		VectorClock ret = new VectorClock(osr.getDim());
		int threadId = event.first;
		int inThreadId = event.second;
		int numThreads = ret.getDim();
		int limit = osr.getClockIndex(threadId);

		// init with direct edges
		for(int i=0; i<this.width; i++){
			if(i == threadId) {
				ret.setClockIndex(i, inThreadId);
				continue;
			}

			RangeMinimaQuery rangeMinima = successors.get(threadId).get(i);

			int firstInToThread = rangeMinima.getMinWithRange(inThreadId, limit);
			ret.setClockIndex(i, firstInToThread);
		}

//		====== This is the deprecated O(T^3) updating algo ======
//		for(int i=0; i<this.width; i++){
//			boolean hasChanged = false;
//
//			for(int from=0; from < this.width; from++){
//				if(from == threadId) continue;
//
//				for(int to=0; to < this.width; to++){
//					if (from == to || to == threadId) continue;
//					int left = ret.getClockIndex(from);
//
//					// left should exist in ssp, and not be infinite (-1)
//					if(left != -1 && left <= osr.getClockIndex(from)){
//						int curVal = this.successors.get(from).get(to).getMinWithRange(left, osr.getClockIndex(from));
//						if(curVal != -1 && curVal < ret.getClockIndex(to)) {
//							ret.setClockIndex(to, curVal);
//							hasChanged = true;
//						}
//					}
//				}
//			}
//
//			if(!hasChanged) break;
//		}

//		===== This is the O(T^2) updating algo in the paper =====
		HashSet<Integer> unvisited = new HashSet<>();
		for(int i=0; i<numThreads; i++) {
			if (i != threadId) unvisited.add(i);
		}

		while (!unvisited.isEmpty()) {
			int curTh = -1;
			long minAuxId = -1L;

			// pick earliest curTh
			for(Integer i : unvisited) {
				ArrayList<Long> map = inThreadIdToAuxId[i];
				int localId = ret.getClockIndex(i) - 1;
				long curAuxId;

				// localId = -2  => not reachable for now
				if (localId != -2) {
					curAuxId = map.get(localId);
				} else {
					curAuxId = Long.MAX_VALUE;
				}

				if (curTh == -1 || minAuxId > curAuxId) {
					curTh = i;
					minAuxId = curAuxId;
				}
			}

			// pick curTh to update the rest threads
			unvisited.remove(curTh);
			int left = ret.getClockIndex(curTh);
			int right = osr.getClockIndex(curTh);

			for(Integer to : unvisited) {
				int curVal = this.successors.get(curTh).get(to).getMinWithRange(left, right);
				if(curVal != -1 && curVal < ret.getClockIndex(to)) {
					ret.setClockIndex(to, curVal);
				}
			}
		}

		return ret;
	}

	// This is the entry method for building the abstract graph.
	// Given a list of events (events) and current OLClosure (osr),
	// returns a vector clock vc for each event e in events,
	// where vc[i] = e's first successor in thread i
	public List<VectorClock> queryForEventLists(List<Triplet<Integer, Integer, Long>> events, VectorClock osr,
												ArrayList<Long>[] inThreadIdToAuxId){
		// input = list( <threadId, inThreadId, auxId> )
		//

		List<VectorClock> ret = new ArrayList<>();

		for(Triplet<Integer, Integer, Long> cur : events) {
			ret.add(this.queryForEvent(cur, osr, inThreadIdToAuxId));
		}

		return ret;
	}
}
