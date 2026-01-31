package engine.racedetectionengine.OSR_witness;

import engine.racedetectionengine.RaceDetectionEvent;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import util.Triplet;
import util.vectorclock.VectorClock;

import java.lang.reflect.Array;
import java.util.*;

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

        this.onExistForkEvent(state, threadId);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadId];
        curThCnt++;
        eventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadId] = curThCnt;

        state.threadToLockset[t.getId()].add(l.getId());
        curAcqList.add(eventInfo);

        if(state.verifyWitness){
            eventInfo.eventType = EventType.ACQUIRE;
            eventInfo.lockId = lockId;
            eventInfo.threadId = threadId;
            eventInfo.auxId = this.getAuxId();
            state.eventInfo[threadId].add(eventInfo);
            state.auxIdToEvents.add(eventInfo);
        }

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

        if(state.verifyWitness){
            relEventInfo.eventType = EventType.RELEASE;
            relEventInfo.lockId = lockId;
            relEventInfo.threadId = threadId;
            relEventInfo.auxId = this.getAuxId();
            state.eventInfo[threadId].add(relEventInfo);
            state.auxIdToEvents.add(relEventInfo);
        }

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

        this.onExistForkEvent(state, threadIdx);
        accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        accessEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadIdx] = curThCnt;

        VectorClock lastWriteTLC = state.recentWriteMap.get(v.getId());

        if (lastWriteTLC != null) {
            VectorClock tlc = state.clockThread[threadIdx];
            tlc.updateWithMax(tlc, lastWriteTLC);
        }

        if(state.verifyWitness){
            accessEventInfo.eventType = EventType.READ;
            accessEventInfo.lastWrite = state.recentWriteEvent.get(varId);
            accessEventInfo.varId = varId;
            accessEventInfo.auxId = this.getAuxId();
            accessEventInfo.threadId = threadIdx;
            state.eventInfo[threadIdx].add(accessEventInfo);
            state.auxIdToEvents.add(accessEventInfo);
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

        this.onExistForkEvent(state, threadIdx);

        accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

        this.updateTLClosure(state);


        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        accessEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadIdx] = curThCnt;

        VectorClock recentWriteTLC = state.recentWriteMap.get(v.getId());
        VectorClock prevTLC = state.clockThread[threadIdx];

        recentWriteTLC.copyFrom(prevTLC);
        state.recentWriteEvent.put(varId, auxId);

        if(state.verifyWitness){
            accessEventInfo.eventType = EventType.WRITE;
            accessEventInfo.varId = varId;
            accessEventInfo.auxId = this.getAuxId();
            accessEventInfo.threadId = threadIdx;
            state.eventInfo[threadIdx].add(accessEventInfo);
            state.auxIdToEvents.add(accessEventInfo);
        }

        return checkWrite(state, verbosity, accessEventInfo);
    }

    @Override
    public boolean HandleSubFork(OSRState state, int verbosity) {
        int targetThId = this.getTarget().getId();
        int threadIdx = this.getThread().getId();

        this.onExistForkEvent(state, threadIdx);
        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        VectorClock forkTLC = new VectorClock(state.clockThread[threadIdx]);
        state.threadToForkEvent[targetThId] = forkTLC;

        if(state.verifyWitness){
            EventInfo eventInfo = new EventInfo();
            eventInfo.eventType = EventType.FORK;
            eventInfo.threadId = threadIdx;
            eventInfo.auxId = this.getAuxId();
            state.eventInfo[threadIdx].add(eventInfo);
            state.auxIdToEvents.add(eventInfo);
        }

        return false;
    }

    @Override
    public boolean HandleSubJoin(OSRState state, int verbosity) {
        Thread target = this.target;
        int targetThreadId = target.getId();
        int threadIdx = this.getThread().getId();
        this.onExistForkEvent(state, threadIdx);
        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        VectorClock prevTLC = state.clockThread[threadIdx];
        VectorClock targetTLC = state.clockThread[targetThreadId];

        prevTLC.updateWithMax(prevTLC, targetTLC);

        if(state.verifyWitness){
            EventInfo eventInfo = new EventInfo();
            eventInfo.eventType = EventType.JOIN;
            eventInfo.threadId = threadIdx;
            eventInfo.auxId = this.getAuxId();
            state.eventInfo[threadIdx].add(eventInfo);
            state.auxIdToEvents.add(eventInfo);
        }

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

    // check if e2 (a write event) is in race with a previous read or write event
    public boolean checkWrite(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccessTwoLists(state, verbosity, state.eventsVarsRead, state.eventsVarsWrite, e2);
    }

    // Check if e2 (a write or read event) is in race with a previous read or write event in events
    // Note that we save read and write in different lists
    public boolean checkAccess(OSRState state, int verbosity, HashMap<Integer, ArrayList<AccessEventInfo>>[] events, AccessEventInfo e2) {
        int varId = this.variable.getId();

        for (int thId=0; thId<state.numThreads; thId++) {
            if (thId == this.getThread().getId() || events[thId].get(varId).size() == 0) continue;

            ArrayList<AccessEventInfo> eventsInTh = events[thId].get(varId);

            int start = binarySearchByTLC(state, eventsInTh, e2, thId);

            state.osrEventSet.updateWithMax(state.osrEventSet, e2.prevTLC);

            for (int pos = start; pos < eventsInTh.size(); pos++) {
                AccessEventInfo e1 = eventsInTh.get(pos);

                if(checkEventInVectorstamp(state.osrEventSet, e1, thId)){
                    continue;
                }

                if (checkRace(e1, e2, thId, this.getThread().getId(), state)) {
                    System.out.println("Found race between events with global id " + e1.auxId + " and " +
                            e2.auxId + ", the source loc are " + e1.location + " and " + e2.location);
                    System.out.println();
                    state.racyEvents.add(e2.auxId);
                    state.racyLocations.add(e2.location);
                    reInit(state);
                    return true;
                }
            }

            reInit(state);
        }
        return false;
    }

    // Check if e2 (a write or read event) is in race with a previous read or write event in events
    // Note that we save read and write in different lists.
    // That's why here we have an input read list and another input write list
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
                    System.out.println("Found race between events with global id " + e1.auxId + " and " +
                            e2.auxId + ", the source loc are " + e1.location + " and " + e2.location);
                    System.out.println();
                    state.racyLocations.add(e2.location);
                    state.racyEvents.add(e2.auxId);
                    reInit(state);
                    return true;
                }
            }

            reInit(state);
        }
        return false;
    }

    // entry method for checking races with a read event e2
    public boolean checkRead(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccess(state, verbosity, state.eventsVarsWrite, e2);
    }

    // Check if event toCheck is contained in VectorClock v
    public boolean checkEventInVectorstamp(VectorClock v, EventInfo toCheck, int thIdx) {
        return v.getClock().get(thIdx) >= toCheck.inThreadId;
    }

    // Check if current event set contains more than 1 open acq for each lock
    public boolean checkOpenAcquires(OSRState state) {
        for(Lock lock : state.locks){
            short openNum = state.lockToOpenAcquireNum.get(lock.getId());
            if(openNum > 1) return false;
        }

        return true;
    }

    // Find the earliest event e1 in events1 by binary search,
    // s.t. e1 is not in the TLClosure of e2
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

    // For given two events e1, e2, check if they are in race
    public boolean checkRace(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        calcOSR(e1, e2, e1ThId, e2ThId, state);

        if (checkEventInVectorstamp(state.osrEventSet, e1, e1ThId)) {
            return false;
        }

        if (!checkOpenAcquires(state)) {
            return false;
        }

        List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges = getAllBackwardEdges(state);

        boolean hasCycle = this.buildGraph(state, backwardEdges, e1, e2, e1ThId, e2ThId);

        if(!hasCycle){
            System.out.println("Checking witness for " + e1.auxId + ", " + e2.auxId);
            this.checkWitness(state, backwardEdges, e1.auxId, e2.auxId);
        }

        return !hasCycle;
    }

    // For given e1, e2, build the abstract graph
    public boolean buildGraph(OSRState state, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges,
                              AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId){
        List<Triplet<Integer, Integer, Long>> nodes = new ArrayList<>(); // <thId, inThreadId-1, auxId>
        SimpleDirectedGraph<Long, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

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

        List<VectorClock> vectorClocks = state.partialOrder.queryForEventLists(nodes, state.osrEventSet, state.inThreadIdToAuxId);

        for (int i=0; i < nodes.size(); i++) {
            Triplet<Integer, Integer, Long> curNode = nodes.get(i);
            VectorClock vc = vectorClocks.get(i);

            for (int j=0; j<nodes.size(); j++){
                if(i == j) continue;

                Triplet<Integer, Integer, Long> testNode = nodes.get(j);

                if(testNode.first.intValue() == curNode.first.intValue()) {
                    if (testNode.second > curNode.second) {
                        graph.addEdge(curNode.third, testNode.third);
                    }
                } else if (vc.getClockIndex(testNode.first) != -1 && testNode.second >= vc.getClockIndex(testNode.first)) {
                    graph.addEdge(curNode.third, testNode.third);
                }
            }
        }

        state.graph = graph;

        CycleDetector<Long, DefaultEdge> cd = new CycleDetector<>(graph);

        return cd.detectCycles();
    }

    // For given e1, e2, compute the OLClosure
    public void calcOSR(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        state.osrEventSet.updateWithMax(state.osrEventSet, e1.prevTLC);

        if (checkEventInVectorstamp(state.osrEventSet, e1, e1ThId)) {
            return;
        }

        boolean hasChanged = true;

        while (hasChanged) {
            hasChanged = false;
            for (Lock l : state.locks) {
                int lockId = l.getId();
                boolean[] openAcq = state.openAcquiresExist.get(lockId);
                int[] curAcqListPtrs = state.acqListPtr.get(lockId);

                for (int thId=0; thId<state.numThreads; thId++) {
                    ArrayList<AcqEventInfo> acqs = state.acqList.get(lockId)[thId];
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
                            hasOpenAcq = true;
                            break;
                        } else {
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
    }

    // After finishing checking races for a given e and a thread t,
    // reinit all the data structures to start over
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

    // For a given OSRClosure, find all last-release -> open acquire edges
    // For each backward edge, return a triplet <lockId, open acq event, thread id of open acq event>
    private List<Triplet<Integer, AcqEventInfo, Integer>> getAllBackwardEdges(OSRState state) {
        List<Triplet<Integer, AcqEventInfo, Integer>> ret = new ArrayList<>();

        for (Lock l : state.locks) {
            int lockId = l.getId();
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
                    break;
                }
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

    public void checkWitness(OSRState state, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges, long e1AuxId, long e2AuxId){
        List<Triplet<Integer, Integer, Long>> linearization = this.linearize(state, backwardEdges);

        this.proveWitness(state, linearization, e1AuxId, e2AuxId);


        System.out.println("The witness is constructed: ");

        for(Triplet<Integer, Integer, Long> event : linearization){
            System.out.print("(" + event.third + ") ");
        }
        System.out.println();
    }

    public void proveWitness(OSRState state, List<Triplet<Integer, Integer, Long>> linearization, long e1AuxId, long e2AuxId){
        if(!this.proveTO(state, linearization)){
            System.out.println("Linearization fail to respect TO");
            System.exit(1);
        }

        if(!this.proveRF(state, linearization)){
            System.out.println("Linearization fail to respect RF");

            for(Triplet<Integer, Integer, Long> event : linearization){
                System.out.print("(" + event.third + ") ");
            }
            System.out.println();
            System.exit(1);
        }

        if(!this.proveLockExclusion(state, linearization)){
            System.out.println("Linearization fail to respect Lock Semantics");
            long cnt = 0L;
            boolean flag = false;
            long prev = -1L;
            for(Triplet<Integer, Integer, Long> event : linearization){
                if(cnt > 0 && prev > event.third){
                    flag = true;
                }

                if(flag) System.out.print("(" + event.third + ") ");
                cnt++;
                prev = event.third;
            }
            System.out.println();
            System.exit(1);
        }

        System.out.println("Verify Success !    (" + e1AuxId + ", " + e2AuxId + ")");
    }

    public boolean proveTO(OSRState state, List<Triplet<Integer, Integer, Long>> linearization){
        int[] max = new int[state.numThreads];

        for(Triplet<Integer, Integer, Long> event : linearization){
            int thId = event.first;
            int inThId = event.second;

            if(max[thId] + 1 != inThId){
                return false;
            }

            max[thId]  = inThId;
        }

        return true;
    }

    public boolean proveRF(OSRState state, List<Triplet<Integer, Integer, Long>> linearization){
        HashMap<Integer, Long> lastWriteMap = new HashMap<>();

        for(Triplet<Integer, Integer, Long> event : linearization){
            int thId = event.first;
            int inThId = event.second;

            EventInfo eventInfo = state.eventInfo[thId].get(inThId - 1);

            if(eventInfo.eventType.isWrite()){
                AccessEventInfo temp = (AccessEventInfo) eventInfo;
                if(!lastWriteMap.containsKey(temp.varId)) {
                    lastWriteMap.put(temp.varId, -1L);
                }

                lastWriteMap.put(temp.varId, eventInfo.auxId);
            } else if(eventInfo.eventType.isRead()){
                AccessEventInfo temp = (AccessEventInfo) eventInfo;
                long lastWriteInfo = temp.lastWrite;

                if(!lastWriteMap.containsKey(temp.varId)){
                    lastWriteMap.put(temp.varId, -1L);
                }

                long curLastWrite = lastWriteMap.get(temp.varId);
                if(lastWriteInfo != curLastWrite) {
                    System.out.println("varId = " + temp.varId);
                    System.out.println("read with auxId = " + temp.auxId + " expects : " + lastWriteInfo + ", but got " + curLastWrite);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean proveLockExclusion(OSRState state, List<Triplet<Integer, Integer, Long>> linearization){
        HashMap<Integer, int[]> openAcqs = new HashMap<>();

        for(Triplet<Integer, Integer, Long> event : linearization){
            int thId = event.first;
            int inThId = event.second;

            EventInfo eventInfo = state.eventInfo[thId].get(inThId - 1);

            if(eventInfo.eventType.isAcquire()){
                AcqEventInfo temp = (AcqEventInfo) eventInfo;
                if(!openAcqs.containsKey(temp.lockId)) {
                    openAcqs.put(temp.lockId, new int[]{-1, -1});
                }

                if(openAcqs.get(temp.lockId)[0] != -1){
                    // multiple open acq on the same lock
                    int[] openAcq1 = openAcqs.get(temp.lockId);
                    System.out.println("multiple open acq on the same lock :" + event.third + ", " + state.eventInfo[openAcq1[0]].get(openAcq1[1]-1).auxId);
                    return false;
                }

                openAcqs.get(temp.lockId)[0] = thId;
                openAcqs.get(temp.lockId)[1] = inThId;
            } else if(eventInfo.eventType.isRelease()){
                RelEventInfo temp = (RelEventInfo) eventInfo;
                if(!openAcqs.containsKey(temp.lockId)){
                    // no matching acq
                    System.out.println("no matching acq");
                    return false;
                } else {
                    int[] acqO = openAcqs.get(temp.lockId);
                    if(acqO[0] != thId || acqO[1] >= inThId) {
                        // not in same thread or doesn't respect TO
                        System.out.println("not in same thread or doesn't respect TO");
                        return false;
                    }

                    // set open acq to be null
                    openAcqs.get(temp.lockId)[0] = -1;
                    openAcqs.get(temp.lockId)[1] = -1;
                }
            }
        }
        return true;
    }

    public List<Triplet<Integer, Integer, Long>> linearize(OSRState state, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges){
        // return list(<thId, inThId, auxId>)
        List<Triplet<Integer, Integer, Long>> ret = new ArrayList<>();
        int[] enabled = new int[state.numThreads];
        Arrays.fill(enabled, 1); // the first events in every threads are enabled in the beginning

        Triplet<Integer, Integer, Long> top = getTop(state, enabled, backwardEdges);

        while(top != null){
            ret.add(top);
            enabled[top.first]++;
            top = getTop(state, enabled, backwardEdges);
        }

        // check the set is fully linearized
        for(int i=0; i<state.numThreads; i++){
            if(enabled[i] != state.osrEventSet.getClockIndex(i) + 1){
                System.out.println("Didn't linearize all events in the OSR set");
                System.exit(1);
            }
        }

        return ret;
    }

    public Triplet<Integer, Integer, Long> getTop(OSRState state, int[] enabled, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges){
        // return [thId, inThId]
        // general idea : build a graph, nodes are enabled events in each threads
        // edges : e1 -> e2 iff there is a path from e1 to e2 in SSP set
        // build graph, pick the nodes with (1) indegree = 0,  (2) smallest aux id
        VectorClock osrSet = state.osrEventSet;
        Triplet<Integer, Integer, Long> top = null;

        HashMap<Integer, Integer> eventToIndegree = new HashMap<>();

        // add enabled events into graph
        for(int thId=0; thId<state.numThreads; thId++) {
            if (enabled[thId] <= osrSet.getClockIndex(thId)) {
                eventToIndegree.put(thId, 0);
            }
        }

        for(Integer fromThId : eventToIndegree.keySet()){
            for(Integer toThId : eventToIndegree.keySet()){
                if(!Objects.equals(fromThId, toThId)) {
                    long fromAuxId = state.eventInfo[fromThId].get(enabled[fromThId] - 1).auxId;
                    long toAuxId = state.eventInfo[toThId].get(enabled[toThId] - 1).auxId;
                    Triplet<Integer, Integer, Long> fromNode = new Triplet<>(fromThId, enabled[fromThId], fromAuxId);
                    Triplet<Integer, Integer, Long> toNode = new Triplet<>(toThId, enabled[toThId], toAuxId);

                    if(this.existPath(state, fromNode, toNode)){
                        eventToIndegree.put(toThId, eventToIndegree.get(toThId) + 1);
                    }
                }
            }
        }

        long minAuxId = Long.MAX_VALUE;
        int retThreadId = -1;

        for(Integer thId : eventToIndegree.keySet()){
            if(eventToIndegree.get(thId) == 0){
                long currAuxId = state.eventInfo[thId].get(enabled[thId] - 1).auxId;

                if(retThreadId == -1 || minAuxId > currAuxId){
                    retThreadId = thId;
                    minAuxId = currAuxId;
                }
            }
        }

//        System.out.println(eventToIndegree);
        if(retThreadId == -1){
            return null;
        } else {
            top = new Triplet<>(retThreadId, enabled[retThreadId], state.eventInfo[retThreadId].get(enabled[retThreadId] - 1).auxId);
            return top;
        }
    }


    public boolean existsEdge(OSRState state, Triplet<Integer, Integer, Long> fromEvent, Triplet<Integer, Integer, Long> toEvent){
        // returns true, iff fromEvent -> v1 -> v2 -> toEvent
        SimpleDirectedGraph<Long, DefaultEdge> graph = state.graph;

        Set<Long> vertexSet = graph.vertexSet();

        for(Long e1Prime : vertexSet){
            EventInfo e1 = state.auxIdToEvents.get(Math.toIntExact(e1Prime));

            if(e1Prime.equals(fromEvent.third) || state.partialOrder.existsEdge(fromEvent, new Triplet<>(e1.threadId, e1.inThreadId, e1.auxId),
                    state.osrEventSet, state.inThreadIdToAuxId)){
                for(Long e2Prime : vertexSet){
                    if(e1Prime.equals(e2Prime) || this.existsPathInGraph(state, e1Prime, e2Prime)){
                        EventInfo e2 = state.auxIdToEvents.get(Math.toIntExact(e2Prime));
                        if(e2Prime.equals(toEvent.third) || state.partialOrder.existsEdge(new Triplet<>(e2.threadId, e2.inThreadId, e2.auxId), toEvent, state.osrEventSet, state.inThreadIdToAuxId)){
                            return true;
                        }
                    }
                }
            }
        }


//        System.out.println(fromEvent + "  " + toEvent + "  no edge");
        return false;
    }

    public boolean existsPathInGraph(OSRState state, long start, long end){
        // given a graph, return true if there is a path from start to end
        Set<DefaultEdge> edges = state.graph.edgeSet();
        Set<Long> reachable = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(start);
        reachable.add(start);

        while(queue.size() > 0){
            long cur = queue.poll();
            for(DefaultEdge edge : edges){
                if (state.graph.getEdgeSource(edge) == cur){
                    long next = state.graph.getEdgeTarget(edge);
                    if(!reachable.contains(next)){
                        reachable.add(next);
                        queue.offer(next);
                    }
                }
            }
        }
        return reachable.contains(end);
    }

    public boolean existPath(OSRState state, Triplet<Integer, Integer, Long> fromEvent, Triplet<Integer, Integer, Long> toEvent){
        // given two events, return true if there is a path from -> to in the SSP event set
        if (state.partialOrder.existsEdge(fromEvent, toEvent, state.osrEventSet, state.inThreadIdToAuxId)) {
            return true;
        } else if (this.existsEdge(state, fromEvent, toEvent)){
            return true;
        }
        return false;
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
