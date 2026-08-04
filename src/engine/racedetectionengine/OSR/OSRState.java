package engine.racedetectionengine.OSR;


import engine.racedetectionengine.OSR.POBuild.PartialOrder;
import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import util.vectorclock.VectorClock;

import java.lang.reflect.Array;
import java.util.*;

public class OSRState extends State {

    // Internal Data Structure
    // 1. Number of Threads / Variables / Locks
    public int numThreads;
    public int numVariables;
    public int numLocks;


    // 2. Most recent event / threadEvent
    public HashMap<Integer, VectorClock> recentWriteMap; // variable name -> recent write VC
    public HashMap<Integer, RelEventInfo> recentRelMapAlgo; // lock id -> recent rel ID for algorithm;
    public HashMap<Integer, Integer> recentRelThreadId; // lock id -> recent rel thread id


    // 3. Fork / Join Relation
    public VectorClock[] threadToForkEvent; // threadId -> TLC


    // 4. Thread index mapping
    public int[] threadIdToCnt;  // threadId -> cur num of events in this thread


    // 5. All events in list
    public HashMap<Integer, ArrayList<AccessEventInfo>>[] eventsVarsRead; // thId -> (varId -> List(Reads))
    public HashMap<Integer, ArrayList<AccessEventInfo>>[] eventsVarsWrite; // thId -> (varId -> List(Writes))
    public ArrayList<Long>[] inThreadIdToAuxId; // thId -> (localId -> auxId)

    // 6. LockSet for variables
    public HashSet<Integer>[] threadToLockset;



    // Data Structure for algorithms
    // 1. ThreadSet
    public HashSet<Thread> threads;
    public HashSet<Lock> locks;
    public HashSet<Variable> variables;


    // 2. Vector Clock for each thread
    public VectorClock[] clockThread;

    // 3. AcqList for each lock and thread;
    public HashMap<Integer, ArrayList<AcqEventInfo>[]> acqList;  // lockId -> ( threadId -> ArrayList<acqId> )
    public HashMap<Integer, int[]> acqListPtr; // lockId -> (threadId -> first acq not in S)

    // 4. Bad Acquires
    public HashMap<Integer, boolean[]> openAcquiresExist; // lockId -> (thId -> has open acq in (lockId, thId)?
    public HashMap<Integer, Short> lockToOpenAcquireNum; // lockId -> num of open acquires

    // 5. Data races;
    public HashSet<Long> racyEvents;
    public HashSet<Integer> racyLocations;

    // 6. OSR event set
    public VectorClock osrEventSet;


    // 7. Graph related
    public PartialOrder partialOrder;
    public SimpleDirectedGraph<Long, DefaultEdge> graph;


    // 8. race distance
    public HashMap<Long, Integer> raceDistances;

    public OSRState(){}

    public OSRState(Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode, Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode, int numThreads, ArrayList<Long>[] inThreadIdToAuxId, int i) {
        this.initInternalData(numThreads, succFromNode, succToNode, inThreadIdToAuxId);
        this.initData(numThreads);
    }

    private void initData(int numThreads) {
        // part.1
        this.locks = new HashSet<>();
        this.threads = new HashSet<>();
        this.variables = new HashSet<>();

        // part.2
        this.clockThread = new VectorClock[this.numThreads];
        for(int i=0;i<numThreads;i++){
            this.clockThread[i] = new VectorClock(this.numThreads);
        }

        // part.3
        this.acqList = new HashMap<>();
        this.acqListPtr = new HashMap<>();


        // part.4
        this.openAcquiresExist = new HashMap<>();
        this.lockToOpenAcquireNum = new HashMap<>();

        // part.5
        this.racyEvents = new HashSet<>();
        this.racyLocations = new HashSet<>();

        // part.6
        this.osrEventSet = new VectorClock(this.numThreads);
    }

    private void initInternalData(int numThreads, Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode,
                                  Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode, ArrayList<Long>[] inThreadIdToAuxId) {

        // part.1
        this.numThreads = numThreads;
        this.numVariables = 0;
        this.numLocks = 0;


        // part.2
        this.recentWriteMap = new HashMap<>();
        this.recentRelMapAlgo = new HashMap<>();
        this.recentRelThreadId = new HashMap<>();


        // part.3
        this.threadToForkEvent = new VectorClock[this.numThreads];


        // part.4
        this.threadIdToCnt = new int[numThreads];


        // part.5
        this.eventsVarsRead = (HashMap<Integer, ArrayList<AccessEventInfo>>[]) Array.newInstance(HashMap.class, this.numThreads);
        this.eventsVarsWrite = (HashMap<Integer, ArrayList<AccessEventInfo>>[]) Array.newInstance(HashMap.class, this.numThreads);

        for(int i=0; i<inThreadIdToAuxId.length; i++) {
             Collections.reverse(inThreadIdToAuxId[i]);
        }

        this.inThreadIdToAuxId = inThreadIdToAuxId;
        for(int i=0;i<numThreads;i++){
            this.eventsVarsRead[i] = new HashMap<>();
            this.eventsVarsWrite[i] = new HashMap<>();
        }


        // part.6
        this.threadToLockset = (HashSet<Integer>[]) Array.newInstance(HashSet.class, this.numThreads);
        for(int i=0;i<numThreads;i++) {
            this.threadToLockset[i] = new HashSet<>();
        }


        // part.7
        this.partialOrder = new PartialOrder(succFromNode, succToNode, this.numThreads);


        // part.8
        this.raceDistances = new HashMap<>();
    }

    @Override
    public void printMemory() {

    }
}
