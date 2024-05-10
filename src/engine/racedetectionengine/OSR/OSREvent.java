package engine.racedetectionengine.OSR;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import util.Triplet;
import util.vectorclock.VectorClock;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OSREvent extends RaceDetectionEvent<OSRState> {

    public OSREvent() {
        super();
    }

    @Override
    public boolean Handle(OSRState state, int verbosity) {
        return this.HandleSub(state, verbosity);
    }

    public void onNewLockFound(OSRState state){
        if(!state.locks.contains(this.lock)){
            state.locks.add(this.getLock());
            state.numLocks++;
            int lockId = this.getLock().getId();

            state.acqList.put(lockId, (ArrayList<AcqEventInfo>[]) Array.newInstance(ArrayList.class, state.numThreads));
            state.acqListPtr.put(lockId, new int[state.numThreads]);
            state.openAcquiresExist.put(lockId, new boolean[state.numThreads]);
            state.lockToOpenAcquireNum.put(lockId, (short) 0);


            for(int i=0; i<state.numThreads; i++){
                state.acqList.get(lockId)[i] = new ArrayList<>();
                state.acqListPtr.get(lockId)[i] = 0;
                state.openAcquiresExist.get(lockId)[i] = false;
            }
        }
    }

    public void onNewVarFound(OSRState state){
        Variable v = this.getVariable();
        int varId = v.getId();
        if (!state.variables.contains(v)) {
            state.variables.add(v);
            state.recentWriteMap.put(varId, new VectorClock(state.numThreads));

            for(int thId=0; thId<state.numThreads; thId++){
                state.eventsVarsRead[thId].put(varId, new ArrayList<>());
                state.eventsVarsWrite[thId].put(varId, new ArrayList<>());
            }
        }
    }

    @Override
    public boolean HandleSubAcquire(OSRState state, int verbosity) {
        Lock l = this.getLock();
        int lockId = l.getId();

        Thread t = this.getThread();
        int threadId = t.getId();

        this.onNewLockFound(state);

        AcqEventInfo eventInfo = new AcqEventInfo();
        eventInfo.auxId = this.getAuxId();
        ArrayList<AcqEventInfo> curAcqList = state.acqList.get(lockId)[threadId];

        // check fork-join requests
        this.onExistForkEvent(state, threadId);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadId];
        curThCnt++;
        eventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadId] = curThCnt;

        state.threadToLockset[t.getId()].add(l.getId());
        curAcqList.add(eventInfo);

        return false;
    }

    @Override
    public boolean HandleSubRelease(OSRState state, int verbosity) {
        Lock l = this.getLock();
        int lockId = l.getId();

        Thread t = this.getThread();
        int threadId = t.getId();

        this.onNewLockFound(state);
        RelEventInfo relEventInfo = new RelEventInfo();

        this.updateTLClosure(state);


        // update in-thread id and auxId
        int curThCnt = state.threadIdToCnt[threadId];
        curThCnt++;
        relEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadId] = curThCnt;
        relEventInfo.auxId = this.getAuxId();

        ArrayList<AcqEventInfo> curList = state.acqList.get(lockId)[threadId];
        AcqEventInfo matchAcq = curList.get(curList.size() - 1);
        matchAcq.relEventInfo = relEventInfo;
        relEventInfo.TLClosure = new VectorClock(state.clockThread[threadId]);

        state.threadToLockset[t.getId()].remove(l.getId());

        return false;
    }

    @Override
    public boolean HandleSubRead(OSRState state, int verbosity) {
        Thread t = this.getThread();
        Variable v = this.getVariable();
        int threadIdx = this.getThread().getId();
        int varId = v.getId();
        this.onNewVarFound(state);
        AccessEventInfo accessEventInfo = new AccessEventInfo();
        state.eventsVarsRead[this.getThread().getId()].get(varId).add(accessEventInfo);
        accessEventInfo.auxId = this.getAuxId();
        accessEventInfo.location = this.getLocId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);
        accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        accessEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // add entry for last-write map
        VectorClock lastWriteTLC = state.recentWriteMap.get(v.getId());

        if (lastWriteTLC != null) {
            VectorClock tlc = state.clockThread[threadIdx];
            tlc.updateWithMax(tlc, lastWriteTLC);
        }

        return checkRead(state, verbosity, accessEventInfo);
    }



    @Override
    public boolean HandleSubWrite(OSRState state, int verbosity) {
        Variable v = this.getVariable();
        Thread t = this.getThread();
        int threadIdx = t.getId();
        int varId = v.getId();
        this.onNewVarFound(state);

        AccessEventInfo accessEventInfo = new AccessEventInfo();
        state.eventsVarsWrite[threadIdx].get(varId).add(accessEventInfo);
        accessEventInfo.auxId = this.getAuxId();
        accessEventInfo.location = this.getLocId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);

        accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

        this.updateTLClosure(state);


        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        accessEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // update ConfClosure
        VectorClock recentWriteTLC = state.recentWriteMap.get(v.getId());
        VectorClock prevTLC = state.clockThread[threadIdx];


        // update recentWriteMap
        recentWriteTLC.copyFrom(prevTLC);

        return checkWrite(state, verbosity, accessEventInfo);
    }

    @Override
    public boolean HandleSubFork(OSRState state, int verbosity) {
        // add fork request
        int targetThId = this.getTarget().getId();

        // Initialize TLClosure of current event e to be the TlClosure(prev(e)) + e
        int threadIdx = this.getThread().getId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // add fork event to the target thread
        VectorClock forkTLC = new VectorClock(state.clockThread[threadIdx]);
        state.threadToForkEvent[targetThId] = forkTLC;
        return false;
    }

    @Override
    public boolean HandleSubJoin(OSRState state, int verbosity) {
        Thread target = this.target;
        int targetThreadId = target.getId();

        // Initialize TLClosure of current event e to be the TlClosure(prev(e)) + e
        int threadIdx = this.getThread().getId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);

        this.updateTLClosure(state);


        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // update ConfClosure and TLClosure
        VectorClock prevTLC = state.clockThread[threadIdx];
        VectorClock targetTLC = state.clockThread[targetThreadId];

        prevTLC.updateWithMax(prevTLC, targetTLC);

        return false;
    }

    public void onExistForkEvent(OSRState state, int threadIdx){
        VectorClock forkEventTLC = state.threadToForkEvent[threadIdx];

        if (forkEventTLC != null) {
            state.clockThread[threadIdx].updateWithMax(state.clockThread[threadIdx], forkEventTLC);
            state.threadToForkEvent[threadIdx] = null;
        }
    }

    public void updateTLClosure(OSRState state){
        int threadIdx = this.getThread().getId();
        VectorClock prevTLC = state.clockThread[threadIdx];
        int original = prevTLC.getClockIndex(threadIdx);
        prevTLC.setClockIndex(threadIdx, original + 1);
    }


    public boolean checkWrite(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccessTwoLists(state, verbosity, state.eventsVarsRead, state.eventsVarsWrite, e2);
    }

    public boolean checkAccess(OSRState state, int verbosity, HashMap<Integer, ArrayList<AccessEventInfo>>[] events, AccessEventInfo e2) {
        int varId = this.variable.getId();

        for (int thId=0; thId<state.numThreads; thId++) {
            if (thId == this.getThread().getId() || events[thId].get(varId).size() == 0) continue;

            ArrayList<AccessEventInfo> eventsInTh = events[thId].get(varId);

            int start = binarySearchByTLC(state, eventsInTh, e2, thId);

            // update with prev(e2) or fork event of e2
            state.osrEventSet.updateWithMax(state.osrEventSet, e2.prevTLC);

            for (int pos = start; pos < eventsInTh.size(); pos++) {
                AccessEventInfo e1 = eventsInTh.get(pos);

                if(checkEventInVectorstamp(state.osrEventSet, e1, thId)){
                    // e1 \in OSR set, should increase e1 till e1 \notin OSR
                    continue;
                }

                if (checkRace(e1, e2, thId, this.getThread().getId(), state)) {
//                    System.out.println("race between " + e1.auxId + ", " + e2.auxId);
//                    updateRaceDistances(state, e1.auxId, e2.auxId);
//                    System.out.println("OSR set between " + e1.auxId + ", " + e2.auxId + " " + state.osrEventSet);
                    state.racyEvents.add(e2.auxId);
                    state.racyLocations.add(e2.location);
                    reInit(state);
                    return true;
                }
            }

            // re-init data structures that are modified during checkRace
            reInit(state);
        }
        return false;
    }

    public boolean checkAccessTwoLists(OSRState state, int verbosity, HashMap<Integer, ArrayList<AccessEventInfo>>[] readList,
                                       HashMap<Integer, ArrayList<AccessEventInfo>>[] writeList, AccessEventInfo e2) {
        int varId = this.variable.getId();

        for (int thId=0; thId<state.numThreads; thId++) {

            if (thId == this.getThread().getId()) continue;
            else if(readList[thId].get(varId).size() == 0 && writeList[thId].get(varId).size() == 0) continue;

            ArrayList<AccessEventInfo> readsInTh = readList[thId].get(varId);
            ArrayList<AccessEventInfo> writesInTh = writeList[thId].get(varId);

            int posRead = binarySearchByTLC(state, readsInTh, e2, thId);
            int posWrite = binarySearchByTLC(state, writesInTh, e2, thId);
            int readsLimit = readsInTh.size();
            int writesLimit = writesInTh.size();

            // update with prev(e2) or fork event of e2
            state.osrEventSet.updateWithMax(state.osrEventSet, e2.prevTLC);
            boolean isE1Read;
            AccessEventInfo e1;

            while(posRead < readsLimit || posWrite < writesLimit){
                if(posRead >= readsLimit) {
                    isE1Read = false;
                } else if(posWrite >= writesLimit) {
                    isE1Read = true;
                } else if(readsInTh.get(posRead).inThreadId < writesInTh.get(posWrite).inThreadId){
                    isE1Read = true;
                } else {
                    isE1Read = false;
                }

                e1 = isE1Read? readsInTh.get(posRead) : writesInTh.get(posWrite);

                if(isE1Read) posRead++;
                else posWrite++;

                if(checkEventInVectorstamp(state.osrEventSet, e1, thId)){
                    // e1 \in OSR set, should increase e1 till e1 \notin OSR
                    continue;
                }

                if (checkRace(e1, e2, thId, this.getThread().getId(), state)) {
//                    System.out.println("race between " + e1.auxId + ", " + e2.auxId);
//                    updateRaceDistances(state, e1.auxId, e2.auxId);
//                    System.out.println("OSR set between " + e1.auxId + ", " + e2.auxId + " " + state.osrEventSet);
                    state.racyLocations.add(e2.location);
                    state.racyEvents.add(e2.auxId);
                    reInit(state);

                    return true;
                }
            }

            // re-init data structures that are modified during checkRace
            reInit(state);
        }
        return false;
    }

    public boolean checkRead(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccess(state, verbosity, state.eventsVarsWrite, e2);
    }

    public boolean checkEventInVectorstamp(VectorClock v, EventInfo toCheck, int thIdx) {
        return v.getClock().get(thIdx) >= toCheck.inThreadId;
    }


    public boolean checkOpenAcquires(OSRState state) {
        for(Lock lock : state.locks){
            short openNum = state.lockToOpenAcquireNum.get(lock.getId());
            if(openNum > 1) return false;
        }

        return true;
    }

    private int binarySearchByTLC(OSRState state, ArrayList<AccessEventInfo> events1, AccessEventInfo e2, int e1ThId) {
        VectorClock prevE2TLC = e2.prevTLC;
        if (prevE2TLC == null) return 0;

        int left = 0, right = events1.size() - 1;
        int mid = 0;

        while (left < right) {
            mid = (left + right) / 2;
            AccessEventInfo temp = events1.get(mid);
            if (checkEventInVectorstamp(prevE2TLC, temp, e1ThId)) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return mid;
    }


    // given e1 and e2, check whether they are races
    public boolean checkRace(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        // e1 -> curr event   e2 -> cmp event
        calcOSR(e1, e2, e1ThId, e2ThId, state); // state.osrEventSet has been updated

        // if e1 in OSR return false, e2 will never be in OSR
        if (checkEventInVectorstamp(state.osrEventSet, e1, e1ThId)) {
            return false;
        }

        // feasibility check
        if (!checkOpenAcquires(state)) {
            return false;
        }

        List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges = getAllBackwardEdges(state);

        boolean hasCycle = this.buildGraph(state, backwardEdges, e1, e2, e1ThId, e2ThId);

//        if(!hasCycle) {
//            System.out.println("graph size = " + state.graph.vertexSet().size() / 2);
//        }

        return !hasCycle;
    }

    public boolean buildGraph(OSRState state, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges,
                              AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId){
        // backwardEdge = <lockId, acqO, thId of acqO>
        List<Triplet<Integer, Integer, Long>> nodes = new ArrayList<>(); // <thId, inThreadId-1, auxId>
        SimpleDirectedGraph<Long, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        // add nodes and backward edges into the graph
        for(Triplet<Integer, AcqEventInfo, Integer> backwardEdge : backwardEdges){
            int lockId = backwardEdge.first;
            long acqOAuxId = backwardEdge.second.auxId;
            RelEventInfo lastRel = state.recentRelMapAlgo.get(lockId);
            long lastRelAuxId = state.recentRelMapAlgo.get(lockId).auxId;
            int lastRelThId = state.recentRelThreadId.get(lockId);

            graph.addVertex(acqOAuxId);
            nodes.add(new Triplet<>(backwardEdge.third, backwardEdge.second.inThreadId, acqOAuxId));
            graph.addVertex(lastRelAuxId);
            nodes.add(new Triplet<>(lastRelThId, lastRel.inThreadId, lastRelAuxId));
            graph.addEdge(lastRelAuxId, acqOAuxId);
        }

//        System.out.println(nodes);
        List<VectorClock> vectorClocks = state.partialOrder.queryForEventLists(nodes, state.osrEventSet, state.inThreadIdToAuxId);

        // add forward edges
        for (int i=0; i < nodes.size(); i++) {
            Triplet<Integer, Integer, Long> curNode = nodes.get(i);
            VectorClock vc = vectorClocks.get(i);

            for (int j=0; j<nodes.size(); j++){
                if(i == j) continue;

                Triplet<Integer, Integer, Long> testNode = nodes.get(j); // <thId, inThId, auxId>

                if(testNode.first.intValue() == curNode.first.intValue()) {
                    if (testNode.second > curNode.second) {
                        graph.addEdge(curNode.third, testNode.third);
                    }
                } else if (vc.getClockIndex(testNode.first) != -1 && testNode.second >= vc.getClockIndex(testNode.first)) {
//                    System.out.println("add edge : " + curNode.third + " -> " + testNode.third);
                    graph.addEdge(curNode.third, testNode.third);
                }
            }
        }

        state.graph = graph;

        CycleDetector<Long, DefaultEdge> cd = new CycleDetector<>(graph);

        return cd.detectCycles();
    }


    public void calcOSR(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        // e1 -> cmp event   e2 -> curr event
        state.osrEventSet.updateWithMax(state.osrEventSet, e1.prevTLC);

        //e1 in TLC(prev(e2)), no race from e2 onwards
        if (checkEventInVectorstamp(state.osrEventSet, e1, e1ThId)) {
            return;
        }

        // repeat updating OSR
        boolean hasChanged = true;

        while (hasChanged) {
            hasChanged = false;
            // add acq events from acqList
            for (Lock l : state.locks) {
                int lockId = l.getId();
                boolean[] openAcq = state.openAcquiresExist.get(lockId);
                int[] curAcqListPtrs = state.acqListPtr.get(lockId);

                for (int thId=0; thId<state.numThreads; thId++) {

                    ArrayList<AcqEventInfo> acqs = state.acqList.get(lockId)[thId];

                    // get current ptr;  if pointing to the end, skip this loop
                    int ptr = curAcqListPtrs[thId];

                    if (ptr >= acqs.size()) continue;

                    RelEventInfo curRelEvent = null;
                    RelEventInfo toUpdateRelEvent = null;

                    boolean hasOpenAcq = openAcq[thId];

                    boolean initOpenAcq = openAcq[thId];

                    while (ptr < acqs.size()) {
                        AcqEventInfo curAcqEvent = acqs.get(ptr);

                        if (!checkEventInVectorstamp(state.osrEventSet, curAcqEvent, thId)) break; // curAcq not in S  =>  break

                        curRelEvent = curAcqEvent.relEventInfo;

                        if(curRelEvent == null || checkEventInVectorstamp(curRelEvent.TLClosure, e1, e1ThId)
                                || checkEventInVectorstamp(curRelEvent.TLClosure, e2, e2ThId)) {
                            // cannot be added, i.e. Open Acquire

                            hasOpenAcq = true;
                            break;
                        } else {
                            // curRelEvent can be added into OSR
                            hasOpenAcq = false;
                            toUpdateRelEvent = curRelEvent;
                        }

                        ptr++;
                    }

                    if (toUpdateRelEvent != null) {
                        if(!checkEventInVectorstamp(state.osrEventSet, toUpdateRelEvent, thId)){
                            state.osrEventSet.updateWithMax(state.osrEventSet, toUpdateRelEvent.TLClosure);
                        }

                        RelEventInfo curLastRel = state.recentRelMapAlgo.get(lockId);
                        if (curLastRel == null || curLastRel.auxId < toUpdateRelEvent.auxId) {
                            state.recentRelMapAlgo.put(lockId, toUpdateRelEvent);
                            state.recentRelThreadId.put(lockId, thId);
                        }

                        hasChanged = true;
                    }
                    curAcqListPtrs[thId] = ptr;
                    openAcq[thId] = hasOpenAcq;

                    if(initOpenAcq != openAcq[thId]){
                        short prevNum = state.lockToOpenAcquireNum.get(lockId);
                        if(initOpenAcq){
                            state.lockToOpenAcquireNum.put(lockId, (short) (prevNum - 1));
                        } else {
                            state.lockToOpenAcquireNum.put(lockId, (short) (prevNum + 1));
                        }
                    }
                }
            }
        }

//        System.out.println("OSR[" + e1.getAuxId() + ", " + e2.getAuxId()
//                + "] : " + ret);
    }

    public void reInit(OSRState state) {
        state.recentRelMapAlgo.clear();
        state.recentRelThreadId.clear();

        for (Lock l : state.locks) {
            int l_id = l.getId();

            state.lockToOpenAcquireNum.put(l_id, (short) 0);

            int[] curAcqListPtrs = state.acqListPtr.get(l_id);
            boolean[] badAcqListPtrs = state.openAcquiresExist.get(l_id);

            for (int thId=0; thId<state.numThreads; thId++) {
                curAcqListPtrs[thId] = 0;
                badAcqListPtrs[thId] = false;
            }
        }

        state.osrEventSet.setToZero();
    }

    private List<Triplet<Integer, AcqEventInfo, Integer>> getAllBackwardEdges(OSRState state) {
        // return a list of backward edges in OSR set
        // A backward edge = <lockId, acqO, thId of acqO>
        List<Triplet<Integer, AcqEventInfo, Integer>> ret = new ArrayList<>();

        for (Lock l : state.locks) {
            int lockId = l.getId();
            // recent rel(l) = null => no backward edge
            RelEventInfo recentRelEvent = state.recentRelMapAlgo.get(lockId);
            if (recentRelEvent == null) continue;

            boolean[] hasOpenAcq = state.openAcquiresExist.get(lockId);
            int[] acqPtrList = state.acqListPtr.get(lockId);
            long recentRelId = recentRelEvent.auxId;

            for (int thId=0; thId<state.numThreads; thId++) {

                if (hasOpenAcq[thId]) {
                    int acqPos = acqPtrList[thId];
                    AcqEventInfo acqO_Event = state.acqList.get(lockId)[thId].get(acqPos);
                    long curAcqOId = acqO_Event.auxId;

                    if (recentRelId > curAcqOId) {
                        Triplet<Integer, AcqEventInfo, Integer> curPair = new Triplet<>(lockId, acqO_Event, thId);
                        ret.add(curPair);
                    }
                    // previous feasibility check already makes sure there is at most 1 open acq for each lock
                    // therefore, if there is an open acq in curr thread, no matter it's backward edge or not
                    // We can directly break and jump to check the next lock
                    break;
                }
                // no need to check for (second - first > 1), it's done in checkBadAcquires()
            }
        }
        return ret;
    }


    public void updateRaceDistances(OSRState state, Long e1AuxId, Long e2AuxId) {
        long distance = Math.abs(e1AuxId - e2AuxId);
        if (!state.raceDistances.containsKey(distance)) {
            state.raceDistances.put(distance, 0);
        }

        int newVal = state.raceDistances.get(distance) + 1;
        state.raceDistances.put(distance, newVal);
    }


    @Override
    public void printRaceInfoLockType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoAccessType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoExtremeType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoTransactionType(OSRState state, int verbosity) {

    }

    @Override
    public boolean HandleSubBegin(OSRState state, int verbosity) {
        return false;
    }

    @Override
    public boolean HandleSubEnd(OSRState state, int verbosity) {
        return false;
    }
}
