package engine.racedetectionengine.seqpreserving;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.BitSet;

import event.EventType;

public class State {
    public HashMap<Integer, HashMap<String, DependentInfo>> states = new HashMap<>();
    public int numOfThreads;
    public int numOfVariables;
    public int numOfLocks;
    public int raceCnt = 0;
    public boolean racy = false;
    public long timestamp;
    private boolean subsumption;
    private HashMap<Integer, HashMap<Integer, Integer>> lockHold = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, Integer>> lockHold2 = new HashMap<>();
    public HashSet<Integer> racyLocs = new HashSet<>();
    HashSet<Integer> protectedVars;

    public State(int numOfThreads, int numOfVars, int numOfLocks, HashSet<Integer> protectedVars, boolean subsumption) {
        this.numOfThreads = numOfThreads;
        this.numOfVariables = numOfVars;
        this.numOfLocks = numOfLocks;
        this.subsumption = subsumption;
        this.protectedVars = protectedVars;

        DependentInfo dep = new DependentInfo(numOfThreads, numOfVariables, numOfLocks);
        HashMap<String, DependentInfo> substates = new HashMap<>();
        substates.put(dep.hashString, dep);
        states.put(-1, substates);
    };

    public boolean update(PrefixEvent e) {
        int eThr = e.getThread().getId();
        int eDec = -1;
        int eType = e.getType().ordinal();
        if(e.getType().isAccessType()) {
            eDec = e.getVariable().getId();
        }
        else if(e.getType().isLockType()) {
            eDec = e.getLock().getId();
        }
        else if(e.getType().isExtremeType()) {
            eDec = e.getTarget().getId();
        }

        HashMap<Integer, HashMap<String, DependentInfo>> newStates = new HashMap<>();
        boolean matched = false;
        timestamp++;
        if(!lockHold.containsKey(eThr)) {
			lockHold.put(eThr, new HashMap<>());
		}
        if(!lockHold2.containsKey(eThr)) {
			lockHold2.put(eThr, new HashMap<>());
		}

        if(eType == EventType.ACQUIRE.ordinal()) {
            if(!lockHold2.get(eThr).containsKey(eDec)) {
                lockHold2.get(eThr).put(eDec, 0);
            }
			lockHold2.get(eThr).put(eDec, lockHold2.get(eThr).get(eDec) + 1);
		}
		if(eType == EventType.RELEASE.ordinal()) {
			assert(lockHold2.get(eThr).get(eDec) > 0);
            lockHold2.get(eThr).put(eDec, lockHold2.get(eThr).get(eDec) - 1);
            if(lockHold2.get(eThr).get(eDec) == 0) {
                lockHold2.get(eThr).remove(eDec);
            }
		}

        for(int var: states.keySet()) {
            HashMap<String, DependentInfo> substates = states.get(var);
            for(String hashString: substates.keySet()){
                DependentInfo dep = substates.get(hashString);
    
                if(!racy && e.getType().isAccessType() && dep.candVar == eDec && !dep.threads.get(eThr)) {
                    if(dep.isWriteCandidate || eType == EventType.WRITE.ordinal()) {
                        racy = true;
                    }
                }
    
                if(dep.mustIgnore(eThr, eDec, eType)) {
                    if(dep.candVar == -1 && e.getType().isRead() && !protectedVars.contains(eDec) && !dep.threads.get(eThr)) {
                        DependentInfo depCopied = new DependentInfo(dep);
                        depCopied.ignore(eThr, eDec, eType, lockHold);
                        depCopied.candVar = eDec;
                        depCopied.isWriteCandidate = false;
                        depCopied.hashString = depCopied.toString(); 
                        addToStates(newStates, depCopied);
                    }
                    dep.ignore(eThr, eDec, eType, lockHold);
                    
                }
                else {
                    if(((e.getType().isAccessType() && dep.candVar == -1 && !protectedVars.contains(eDec))) || (e.getType().isAcquire() && dep.candVar != -1)) {
                        DependentInfo depCopied = new DependentInfo(dep);
                        if(e.getType().isAccessType()) {
                            depCopied.candVar = eDec;
                            depCopied.isWriteCandidate = e.getType().isWrite();
                        }
                        depCopied.ignore(eThr, eDec, eType, lockHold);
                        depCopied.hashString = depCopied.toString();
                        if(depCopied.threads.size() != numOfThreads) {
                            addToStates(newStates, depCopied);
                        }
                    }
    
                    // II. Keep event e:
                    if(e.getType().isWrite()) {
                        dep.wtVars.clear(eDec);
                    } 
                }
                dep.hashString = dep.toString();
                addToStates(newStates, dep);
            }
        }
        
        states = newStates;
        if(racy) {
            raceCnt++;
            matched = true;
        }
        racy = false;
        if(eType == EventType.ACQUIRE.ordinal()) {
            if(!lockHold.get(eThr).containsKey(eDec)) {
                lockHold.get(eThr).put(eDec, 0);
            }
			lockHold.get(eThr).put(eDec, lockHold.get(eThr).get(eDec) + 1);
		}
		if(eType == EventType.RELEASE.ordinal()) {
			assert(lockHold.get(eThr).get(eDec) > 0);
            lockHold.get(eThr).put(eDec, lockHold.get(eThr).get(eDec) - 1);
            if(lockHold.get(eThr).get(eDec) == 0) {
                lockHold.get(eThr).remove(eDec);
            }
		}
        return matched;
    }

    private void addToStates(HashMap<Integer, HashMap<String, DependentInfo>> states, DependentInfo dep) {
        if(!states.containsKey(dep.candVar)) {
            states.put(dep.candVar, new HashMap<>());
        }
        HashMap<String, DependentInfo> substates = states.get(dep.candVar);
        dep.hashString = dep.toString();
        if(substates.containsKey(dep.hashString)) {
            return;
        }
        if(subsumption) {
            for(Iterator<Entry<String, DependentInfo>> it = substates.entrySet().iterator(); it.hasNext();) {
                DependentInfo depInfo = it.next().getValue();
                if(depInfo.subsume(dep, lockHold2)) {
                    return;
                }
                if(dep.subsume(depInfo, lockHold2)) {
                    it.remove();
                }
            }
        }
        substates.put(dep.hashString, dep);
    }

    public void printMemory() {
        System.out.println(states.size());
    }
}

class DependentInfo {
    public BitSet threads;
    public BitSet wtVars;
    public BitSet openLocks;

    public int candVar;
    public boolean isWriteCandidate;

    public String hashString;

    public DependentInfo(int numOfThr, int numOfVar, int numOfLck) {
        threads = new BitSet(numOfThr);
        wtVars = new BitSet(numOfVar);
        openLocks = new BitSet(numOfLck);
        candVar = -1;
        isWriteCandidate = false;
        hashString = toString();
    }

    public DependentInfo(DependentInfo other) {
        threads = (BitSet)other.threads.clone();
        wtVars = (BitSet)other.wtVars.clone();
        openLocks = (BitSet)other.openLocks.clone();
        candVar = other.candVar;
        isWriteCandidate = other.isWriteCandidate;
    }

    public boolean mustIgnore(int eThr, int eDec, int eType){
        return  (threads.get(eThr)) ||
                (eType == EventType.ACQUIRE.ordinal() && openLocks.get(eDec)) ||
                (eType == EventType.READ.ordinal() && wtVars.get(eDec)) ||
                (eType == EventType.JOIN.ordinal() && threads.get(eDec));
	}

    public void ignore(int eThr, int eDec, int eType, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {
        if(!threads.get(eThr)) {
            for(int lck: lockHold.get(eThr).keySet()) {
                openLocks.set(lck);
            }
        }
        threads.set(eThr);
        if(eType == EventType.WRITE.ordinal()) {
            wtVars.set(eDec);
        }
        if(eType == EventType.FORK.ordinal()) {
            threads.set(eDec);
        }
    }

    private boolean subsume(BitSet b1, BitSet b2) {
        BitSet b1Clone = (BitSet)b1.clone();
        b1Clone.andNot(b2);
        return b1Clone.isEmpty();
    }    

    private boolean subsumeOpenLock(BitSet t1, BitSet t2, BitSet l2, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {
        BitSet t2Clone = (BitSet)t2.clone();
        t2Clone.andNot(t1);
        for(int i = t2Clone.nextSetBit(0); i >= 0; i = t2Clone.nextSetBit(i+1)) {
            if(lockHold.containsKey(i)) {
                for(int lck: lockHold.get(i).keySet()) {
                    if(!l2.get(lck)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean subsume(DependentInfo other, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {

        return  this.candVar == other.candVar &&
                (this.isWriteCandidate || !other.isWriteCandidate) &&
                subsume(this.threads, other.threads) &&
                subsume(this.wtVars, other.wtVars) &&
                subsume(this.openLocks, other.openLocks) && 
                subsumeOpenLock(this.threads, other.threads, other.openLocks, lockHold);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(threads);
        sb.append(wtVars);
        sb.append(openLocks);
        sb.append(candVar);
        sb.append(isWriteCandidate ? "W" : "R");
        return sb.toString();
    }

}

class DependentInfoComparator implements Comparator<DependentInfo> {
    public int compare(DependentInfo o1, DependentInfo o2) {
        return o1.hashString.compareTo(o2.hashString);
    }
}
