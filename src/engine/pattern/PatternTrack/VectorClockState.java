package engine.pattern.PatternTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import util.Pair;

import engine.pattern.State;
import event.Thread;
import event.Lock;
import event.Variable;
import util.vectorclock.VectorClock;

public class VectorClockState extends State {
    private HashMap<Thread, Integer> threadToIndex = new HashMap<>();
    public HashMap<ArrayList<Pair<Thread, Integer>> , ArrayList<VectorClock>> currentState = new HashMap<>();
    public HashMap<Integer, HashSet<Integer>> pattern = new HashMap<>();

    private int numThreads;
    private int k;

    private HashMap<Thread, VectorClock> threadClock = new HashMap<>();
    private HashMap<Variable, VectorClock> readClock = new HashMap<>();
    private HashMap<Variable, VectorClock> writeClock = new HashMap<>();
    private HashMap<Lock, VectorClock> lockClock = new HashMap<>();

    public VectorClockState(HashSet<Thread> tSet, ArrayList<Integer> pattern) {
        numThreads = tSet.size();
        Iterator<Thread> itThread = tSet.iterator();
        int index = 0;
        while(itThread.hasNext()) {
            Thread thr = itThread.next();
            threadToIndex.put(thr, index);
            index++;
            threadClock.put(thr, emptyClock());
        }
        currentState.put(new ArrayList<>(), new ArrayList<>());
        int cnt = 0;
        for(int p : pattern) {
            if(!this.pattern.containsKey(p)) {
                this.pattern.put(p, new HashSet<Integer>());
            }
            this.pattern.get(p).add(cnt++);
        }
        this.k = pattern.size();
    }

    public VectorClock emptyClock() {
        return new VectorClock(numThreads);
    }

    public VectorClock getThreadClock(Thread t) {
        return threadClock.get(t);
    }

    public VectorClock getReadClock(Variable v) {
        if(!readClock.containsKey(v)) {
            readClock.put(v, emptyClock());
        }
        return readClock.get(v);
    }

    public VectorClock getWriteClock(Variable v) {
        if(!writeClock.containsKey(v)) {
            writeClock.put(v, emptyClock());
        }
        return writeClock.get(v);
    }

    public VectorClock getLockClock(Lock l) {
        if(!lockClock.containsKey(l)) {
            lockClock.put(l, emptyClock());
        }
        return lockClock.get(l);
    }

    public int getThreadIndex(Thread thread) {
        return threadToIndex.get(thread);
    }

    public boolean extendWitness(int locId, Thread thread, VectorClock vc) {
        if(pattern.containsKey(locId)) {
            HashMap<ArrayList<Pair<Thread, Integer>> , ArrayList<VectorClock>> newStates = new HashMap<>();
            for (int index : pattern.get(locId)) {
                for(ArrayList<Pair<Thread, Integer>> witness: currentState.keySet()) {
                    if(witnesses(index, vc, witness, currentState.get(witness))) {
                        if(witness.size() == k - 1) {
                            return true;
                        }
                        ArrayList<Pair<Thread, Integer>> extendedWitness = new ArrayList<>();
                        extendedWitness.addAll(witness);
                        extendedWitness.add(new Pair<Thread, Integer>(thread, index));
                        ArrayList<VectorClock> extendedTimeStamps = new ArrayList<>();
                        extendedTimeStamps.addAll(currentState.get(witness));
                        extendedTimeStamps.add(vc);
                        newStates.put(extendedWitness, extendedTimeStamps);
                    }
                }
            }
            currentState.putAll(newStates);
        }
        return false;
    }

    private boolean witnesses(int index, VectorClock vc, ArrayList<Pair<Thread, Integer>> witness, ArrayList<VectorClock> timestamps) {
        Iterator<Pair<Thread, Integer>> itWitness = witness.iterator();
        Iterator<VectorClock> itTimestamp = timestamps.iterator();
        while(itWitness.hasNext()) {
            Pair<Thread, Integer> event2 = itWitness.next();
            if(event2.first.getId() == index) {
                return false;
            }
            VectorClock timeStamp = itTimestamp.next();
            if(event2.first.getId() > index &&
                timeStamp.isLessThanOrEqual(vc)) {
                    return false;
                }
        }
        return true;
    }

    public void printMemory() {
        // System.out.println(currentState.keySet());
    }
}

