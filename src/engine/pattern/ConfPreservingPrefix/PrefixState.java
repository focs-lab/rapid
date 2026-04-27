package engine.pattern.ConfPreservingPrefix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import util.Pair;

import engine.pattern.PatternTrack.VectorClockState;
import event.Lock;
import event.Thread;
import event.Variable;
import engine.pattern.State;

public class PrefixState extends State {
    public ArrayList<Pair<VectorClockState, DependentInfo>> states = new ArrayList<>(); 
    HashSet<Thread> tSet;
    ArrayList<Integer> pattern;

    public PrefixState(HashSet<Thread> tSet, ArrayList<Integer> pattern) {
        states.add(new Pair<VectorClockState, DependentInfo>(new VectorClockState(tSet, pattern), new DependentInfo()));
        this.tSet = tSet;
        this.pattern = pattern;
    };

    public void printMemory() {
        System.out.println(states.size());
    }
}

class DependentInfo implements Serializable {
    HashSet<Thread> tSet = new HashSet<>();
    HashSet<Variable> wr_vars = new HashSet<>();
    HashSet<Lock> rel_locks = new HashSet<>();

    public boolean allThreads(int n) {
        return tSet.size() == n;
    }

    public boolean check_dependency(Thread t) {
        return tSet.contains(t);
    }

    public boolean check_dependency(Variable v) {
        return wr_vars.contains(v);
    }

    public boolean check_dependency(Lock l) {
        return rel_locks.contains(l);
    }

    public void add(Thread t) {
        tSet.add(t);
    }

    public void add(Variable v) {
        wr_vars.add(v);
    }

    public void add(Lock l) {
        rel_locks.add(l);
    }

    public void remove(Variable v) {
        wr_vars.remove(v);
    }

    public void remove(Lock l) {
        rel_locks.remove(l);
    }

}
