package engine.pattern.Bertoni;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import engine.pattern.State;
import event.Thread;
import event.Lock;
import event.Variable;
import util.vectorclock.VectorClock;

public class BertoniState extends State {
    private HashMap<Thread, Integer> threadToIndex = new HashMap<>();
    public HashMap<Integer, HashSet<Integer>> pattern = new HashMap<>();

    private int numThreads;
    private int k;

    private HashMap<Thread, VectorClock> threadClock = new HashMap<>();
    private HashMap<Variable, VectorClock> readClock = new HashMap<>();
    private HashMap<Variable, VectorClock> writeClock = new HashMap<>();
    private HashMap<Lock, VectorClock> lockClock = new HashMap<>();

    public HashMap<Thread, ArrayList<VectorClock>> history = new HashMap<>();
    public ArrayList<TreeMap<Ideal, Integer>> idealToNonTerm = new ArrayList<>();
    public int idealToNonTermCurrentIndex = 0;
    public HashMap<Integer, HashMap<Integer, Integer>> specialSym = new HashMap<>();
    public ArrayList<TreeSet<Ideal>> ideals = new ArrayList<>();
    public int idealsCurrentIndex = 0;

    private ArrayList<Thread> combination = new ArrayList<>();
    private ArrayList<Thread> threadList;

    public BertoniState(HashSet<Thread> tSet, ArrayList<Integer> pattern) {
        numThreads = tSet.size();
        Iterator<Thread> itThread = tSet.iterator();
        int index = 0;
        while(itThread.hasNext()) {
            Thread thr = itThread.next();
            threadToIndex.put(thr, index);
            threadClock.put(thr, emptyClock());
            history.put(thr, new ArrayList<VectorClock>());
            specialSym.put(index, new HashMap<>());
            index++;
        }
        
        idealToNonTerm.add(new TreeMap<>());
        idealToNonTerm.get(0).put(new Ideal(emptyClock()), -1);

        int cnt = 0;
        for(int p : pattern) {
            if(!this.pattern.containsKey(p)) {
                this.pattern.put(p, new HashSet<Integer>());
            }
            this.pattern.get(p).add(cnt++);
        }
        this.k = pattern.size();
        threadList = new ArrayList<Thread>(tSet);
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

    private void addIdealToNonTerm(Ideal ideal, int sym) {
        if(idealToNonTerm.get(idealToNonTermCurrentIndex).size() == Integer.MAX_VALUE - 1) {
            idealToNonTerm.add(new TreeMap<>());
            idealToNonTermCurrentIndex += 1;
        }
        idealToNonTerm.get(idealsCurrentIndex).put(ideal, sym);
    }

    private int findIdealToNonTerm(Ideal ideal) {
        for(TreeMap<Ideal, Integer> map: idealToNonTerm) {
            if(map.containsKey(ideal)){
                return map.get(ideal);
            }
        }
        return idealToNonTerm.get(idealToNonTermCurrentIndex).get(ideal);
    }

    private void addIdeal(Ideal ideal) {
        if(ideals.get(idealsCurrentIndex).size() == Integer.MAX_VALUE - 1) {
            ideals.add(new TreeSet<>());
            idealsCurrentIndex += 1;
        }
        ideals.get(idealsCurrentIndex).add(ideal);
    }

    public boolean computeNonTerm(int locId, Thread thread) {
        if(pattern.containsKey(locId)) {
            specialSym.get(threadToIndex.get(thread)).
                    put(threadClock.get(thread).
                            getClockIndex(threadToIndex.get(thread)), 
                        locId);
        }
        ideals = generateIdeals(threadClock.get(thread), thread);
        
        for(TreeSet<Ideal> idealSet: ideals) {
            for(Ideal ideal: idealSet) {
                int nonterm = -1;
                for(int thr: ideal.getMaximalThreads()) {
                    int maxSym = ideal.getClock().getClockIndex(thr);
                    ideal.decreaseInThread(thr);
                    int sym = findIdealToNonTerm(ideal);
                    if(nonterm < sym) {
                        nonterm = sym;
                    }
                    if(specialSym.get(thr).containsKey(maxSym)) {
                        int loc = specialSym.get(thr).get(maxSym);
                        for(int a_i : pattern.get(loc)) {
                            if(a_i == 0 || (sym == a_i - 1)) {
                                if(a_i == k - 1) {
                                    return true;
                                }
                                if(nonterm < a_i) {
                                    nonterm = a_i;
                                }
                            }
                        }
                    }
                    ideal.increaseInThread(thr);
                }
                if(nonterm >= -1) {
                    addIdealToNonTerm(ideal, nonterm);
                }
            }
        }
        return false;
    }

    private ArrayList<TreeSet<Ideal>> generateIdeals(VectorClock vc, Thread thread) {
        ideals = new ArrayList<>();
        ideals.add(new TreeSet<>());
        idealsCurrentIndex = 0;
        addIdeal(new Ideal(new VectorClock(vc), new HashSet<Integer>(Arrays.asList(threadToIndex.get(thread)))));
        
        generateCombination(0, vc, thread);
        return ideals;
    }

    private void generateCombination(int index, VectorClock vc, Thread thread) {
        for(int i = index; i < threadList.size(); i++) {
            if(threadList.get(i) == thread) {
                continue;
            }
            combination.add(threadList.get(i));
            HashMap<Integer, VectorClock> vcs = new HashMap<>();
            vcs.put(threadToIndex.get(thread), vc);
            generateIdeal(0, vcs, thread);
            generateCombination(i + 1, vc, thread);
            combination.remove(combination.size() - 1);
        }
    }

    private void generateIdeal(int index, HashMap<Integer, VectorClock> vcs, Thread thread) {
        if(index == combination.size()) {
            VectorClock vc = new VectorClock(numThreads);
            vc.updateWithMax(vcs.values().toArray(new VectorClock[0]));
            HashSet<Integer> maxThreads = getMaximalThreads(vcs);
            if(maxThreads.size() == combination.size() + 1) {
                addIdeal(new Ideal(vc, maxThreads));
            }
        }
        else {
            Thread thr = combination.get(index);
            for(int i = vcs.get(threadToIndex.get(thread)).getClockIndex(threadToIndex.get(thr)); i < history.get(thr).size(); i++) {
                vcs.put(threadToIndex.get(thr), history.get(thr).get(i));
                generateIdeal(index + 1, vcs, thread);
                vcs.remove(threadToIndex.get(thr));
            }
        }
    }

    private HashSet<Integer> getMaximalThreads(HashMap<Integer, VectorClock> vcs) {
        HashSet<Integer> maximalThreads = new HashSet<>();
        for(int thr: vcs.keySet()) {
            boolean flag = true;
            for(int thr2: vcs.keySet()) {
                if(thr == thr2) {
                    continue;
                }
                if(vcs.get(thr).isLessThanOrEqual(vcs.get(thr2))) {
                    flag = false;
                    break;
                }
            }
            if(flag) {
                maximalThreads.add(thr);
            }
        }
        return maximalThreads;
    }

    public void printMemory() {
        // System.out.println(idealToNonTerm.keySet().size());
        // System.out.println(ideals.size());
    }
}

class Ideal implements Comparable<Ideal> {

    private VectorClock vectorClock;
    private int sum;
    private HashSet<Integer> maximalThreads;

    public Ideal(VectorClock vc, HashSet<Integer> mt) {
        this(vc);
        maximalThreads = mt;
    }

    public Ideal(VectorClock vc) {
        vectorClock = vc;
        sum = 0;
        for(int i: vectorClock.getClock()) {
            sum += i;
        }
        maximalThreads = new HashSet<>();
    }

    public VectorClock getClock() {
        return vectorClock;
    }

    public HashSet<Integer> getMaximalThreads() {
        return maximalThreads;
    }

    public void decreaseInThread(int threadIndex) {
        vectorClock.setClockIndex(threadIndex, vectorClock.getClockIndex(threadIndex) - 1);
        sum -= 1;
    }

    public void increaseInThread(int threadIndex) {
        vectorClock.setClockIndex(threadIndex, vectorClock.getClockIndex(threadIndex) + 1);
        sum += 1;
    }

    @Override
	public int compareTo(Ideal other) {
        if (!(this.vectorClock.getDim() == other.vectorClock.getDim())) {
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim");
		}
        int sumDiff = this.sum - other.sum;
		if (sumDiff == 0) {
			for(int i = 0; i < this.vectorClock.getDim(); i++) {
                int diff = this.vectorClock.getClockIndex(i) - other.vectorClock.getClockIndex(i);
                if(diff != 0) {
                    return diff;
                }
            }
            return 0;
		} else {
            return sumDiff;
        }
	}

    @Override
    public String toString() {
        return vectorClock.toString() + " " + sum + ", " + maximalThreads.toString();
    }
}
