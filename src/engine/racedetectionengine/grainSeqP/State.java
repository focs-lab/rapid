package engine.racedetectionengine.grainSeqP;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.BitSet;
import event.EventType;

public class State {

    public HashMap<Integer, HashMap<String, DependentInfo>> statesPhase0 = new HashMap<>();
    public HashMap<String, DependentInfo> statesPhase1 = new HashMap<>(); 
    public HashMap<Integer, HashMap<String, DependentInfo>> statesPhase2 = new HashMap<>();
    public HashMap<String, DependentInfo> statesPhase3 = new HashMap<>();  
    public static int numOfThreads;
    public static int numOfVariables;
    public static int numOfLocks;
    public int raceCnt = 0;
    public boolean racy = false;
    public long timestamp;
    private long poolSize;
    private int sizelimit;
    public long stateSize;
    private int group;

    public PriorityQueue<Long> birthsPhase0 = new PriorityQueue<>();
    public PriorityQueue<Long> birthsPhase1 = new PriorityQueue<>();
    public PriorityQueue<Long> birthsPhase2 = new PriorityQueue<>();
    public PriorityQueue<Long> birthsPhase3 = new PriorityQueue<>();  
    private HashMap<Integer, HashMap<Integer, Integer>> lockHold = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, Integer>> lockHold2 = new HashMap<>();
    public HashSet<Integer> racyLocs = new HashSet<>();
    HashSet<Integer> protectedVars;
    HashSet<Long> orderedEvents;
    
    boolean grainPattern = false;
    boolean grainSize = false;
    boolean LRU = false;
    long eventCount;
    int oneChoice = 0;
    int twoChoices = 0;

    public long getSize() {
        return birthsPhase0.size() + birthsPhase1.size() + birthsPhase2.size() + birthsPhase3.size();
    }

    public State(int numOfThrs, int numOfVars, int numOfLcks, boolean doGrainPattern, boolean doGrainSize, int grainSize, boolean doLRU, double LRUsize, HashSet<Integer> protectedVars, HashSet<Long> orderedEvents, int group) {
        numOfThreads = numOfThrs;
        numOfVariables = numOfVars;
        numOfLocks = numOfLcks;
        this.protectedVars = protectedVars;
        this.orderedEvents = orderedEvents;
        this.group = group;

        if(doGrainPattern) {
            grainPattern = true;
        }
        if(doGrainSize) {
            this.grainSize = true;
            sizelimit = grainSize;
            
        }
        if(doLRU) {
            this.LRU = true;
            this.poolSize = (long)LRUsize;
        }
        if(LRU) System.out.println("LRU size " + this.poolSize);
        if(grainPattern) System.out.println("Grain Pattern");
        if(this.grainSize) System.out.println("G1 size " + this.sizelimit);
        System.out.println("Batch Number " + this.group);
        DependentInfo dep0 = new DependentInfo();
        dep0.hashString = dep0.toString();
        HashMap<String, DependentInfo> substates = new HashMap<>();
        substates.put(dep0.hashString, dep0);
        statesPhase0.put(-1, substates);
        DependentInfo dep1 = new DependentInfo();
        dep1.pin = true;
        dep1.hashString = dep1.toString();
        statesPhase1.put(dep1.hashString, dep1);
    };

    public boolean update(GrainSeqPEvent e) {
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
        eventCount = e.eventCount;

        racy = false;
        timestamp++;
        if(!lockHold.containsKey(eThr)) {
			lockHold.put(eThr, new HashMap<>());
		}
        if(!lockHold2.containsKey(eThr)) {
			lockHold2.put(eThr, new HashMap<>());
		}
        updateLockHold(lockHold2, eThr, eDec, eType);
        stateSize = 0; 
        updatePhase02(eThr, eDec, eType, 0);
        updatePhase1(eThr, eDec, eType);
        updatePhase02(eThr, eDec, eType, 2);
        updatePhase3(eThr, eDec, eType);
        // stateSize = statesPhase0.size() + statesPhase1.size() + statesPhase2.size() + statesPhase3.size();
        updateLockHold(lockHold, eThr, eDec, eType);
        return racy;
    }

    private void updateLockHold(HashMap<Integer, HashMap<Integer, Integer>> lockHold,int eThr, int eDec, int eType) {
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
    }

    private void updatePhase02(int eThr, int eDec, int eType, int phase) {
        HashMap<Integer, HashMap<String, DependentInfo>> states = phase == 0 ? statesPhase0 : statesPhase2;
        HashMap<Integer, HashMap<String, DependentInfo>> newStates = new HashMap<>();
        PriorityQueue<Long> births = phase == 0 ? birthsPhase0 : birthsPhase2;
        PriorityQueue<Long> newBirths = new PriorityQueue<>();
        for(int var: states.keySet()) {
            HashMap<String, DependentInfo> substates = states.get(var);
            for(String hashString: substates.keySet()){
                
                stateSize += 1;
                DependentInfo dep = substates.get(hashString);
                long birth = dep.birth;
                
                if(LRU) {
                    if(!dep.pin && !births.isEmpty() && births.size() >= poolSize - 1 && birth <= births.peek() && birth != 0) {
                        continue;
                    }
                }

                if((phase == 0 && (eType == EventType.ACQUIRE.ordinal() || ((eType == EventType.WRITE.ordinal() || eType == EventType.READ.ordinal()) && choose(eDec)))) || (phase == 2 && (!grainPattern || (eThr != dep.e1Thr && ((eType == EventType.WRITE.ordinal() && dep.e1Var == eDec)|| (eType == EventType.READ.ordinal() && dep.e1Write)) )))) {
                    DependentInfo depCopied = new DependentInfo(dep);
                    if(depCopied.checkLock(eThr, lockHold, phase + 1)){
                        if(phase == 0) {
                            depCopied.pin = true;
                            depCopied.birth = timestamp;
                        }
                        if(phase == 2 && grainPattern) {
                            depCopied.rdFrontier = null;
                            depCopied.acqFrontier = null;
                            // dep.G1Time += 1;
                        }
                        depCopied.hashString = depCopied.toString();
                        HashMap<String, DependentInfo> nextPhase = phase == 0 ? statesPhase1 : statesPhase3; 
                        PriorityQueue<Long> nextBirths = phase == 0 ? birthsPhase1 : birthsPhase3;
                        addToStates13(nextPhase, nextBirths, depCopied, depCopied.birth, phase);
                    }
                }
                if(dep.mustIgnore(eThr, eDec, eType)) {
                    DependentInfo depCopied = new DependentInfo(dep);
                    if(depCopied.ignore(eThr, eDec, eType, lockHold, phase)) {
                        addToStates02(newStates, newBirths, depCopied, birth, phase);
                    }
                }
                else {
                    if(phase == 2) {
                        if(eType == EventType.ACQUIRE.ordinal()/*  && dep.activeAcq < 5*/) {
                            DependentInfo depCopied = new DependentInfo(dep);
                            // depCopied.activeAcq += 1;
                            if(depCopied.ignore(eThr, eDec, eType, lockHold, phase)) {
                                if(birth == 0) {
                                    addToStates02(newStates, newBirths, depCopied, timestamp, phase); 
                                }
                                else {
                                    addToStates02(newStates, newBirths, depCopied, birth, phase);
                                }
                                
                            }
                        }
                    }
                    
                        
                    // }

                    // II. Keep event e:
                    boolean flag = true;
                    DependentInfo depCopied = new DependentInfo(dep); 
                    if (phase == 2) {
                        depCopied.inclThreadsAftG1.set(eThr);
                    }
                    
                    if(eType == EventType.WRITE.ordinal()) {
                        if(phase == 0)
                            depCopied.exclWtVarsBefG1.clear(eDec);
                        else {
                            depCopied.exclWtVarsAftG1.clear(eDec);
                            depCopied.inclWtVarsAftG1.set(eDec);
                            if(depCopied.rdFrontier.containsKey(eDec)) {
                                MazFrontier fr = depCopied.rdFrontier.get(eDec);
                                if(fr.isDependentWithE1) {
                                    flag = false;
                                }
                                else {
                                    depCopied.Frontier.union(fr);
                                }
                            }
                        }
                    } 
                    
                    if(flag) {
                        addToStates02(newStates, newBirths, depCopied, birth, phase);
                    }
                    
                }
            }
        }
        if(phase == 0) {
            statesPhase0 = newStates;
            birthsPhase0 = newBirths;
        }
        else {
            statesPhase2 = newStates;
            birthsPhase2 = newBirths;
        }
    }

    private boolean choose(int eDec) {
        // return true;
        return /*!orderedEvents.contains(eventCount) && */
            eDec % 1 == group && !protectedVars.contains(eDec);
        // if(numOfThreads == 10) {
        //     return !protectedVars.contains(eDec);
        // }
        // else {
        //     return eDec % 94 == group; 
        // }
    }

    private void updatePhase1(int eThr, int eDec, int eType) {
        HashMap<String, DependentInfo> newStates = new HashMap<>();
        PriorityQueue<Long> newBirths = new PriorityQueue<>();
        for(String hashString: statesPhase1.keySet()){
            stateSize += 1;
            DependentInfo dep = statesPhase1.get(hashString);
            long birth = dep.birth;
            // if(eventCount == 9) {
            //     System.out.println(dep.toString());
            // }
            if(LRU) {
                if(!dep.pin && !birthsPhase1.isEmpty() && birthsPhase1.size() >= poolSize - 1 && birth <= birthsPhase1.peek()  && birth != 0) {
                    continue;
                }
            }
            // else if(heuristic == 2) {
            //     if(timestamp - birth >= lifetime) {
            //         continue;
            //     }
            // }
            if(grainSize) {
                if(dep.G1Size + dep.G2Size > sizelimit) {
                    continue;
                }
            }
            boolean force = false;
            if(dep.e1Thr != -1 && (!grainPattern || (dep.G1Size == 1 || (eType == EventType.RELEASE.ordinal() && (eDec == dep.e1Acq))))) {
                force = true;
                DependentInfo depCopied = new DependentInfo(dep);
                if(dep.G1Size == 1) {
                    depCopied.pin = true;
                }
                depCopied.hashString = depCopied.toString();
                if(!statesPhase2.containsKey(depCopied.e1Var)) {
                    statesPhase2.put(depCopied.e1Var, new HashMap<>());
                }
                HashMap<String, DependentInfo> substates = statesPhase2.get(depCopied.e1Var);
                if(!substates.containsKey(depCopied.hashString)) {
                    addToStates02(statesPhase2, birthsPhase2, depCopied, depCopied.birth, 2);
                }
            }
            if(grainPattern && !dep.thrG1.isEmpty() && !dep.thrG1.get(eThr)) {
                continue;
            }
            if(!grainPattern || !force) {
            {
                dep.G1Size += 1;
                if(dep.G1Size > 1 || (eType == EventType.ACQUIRE.ordinal())) {
                DependentInfo depCopied = new DependentInfo(dep);
                if(depCopied.checkLock(eThr, lockHold, 1))  {
                    if(depCopied.G1Size == 1 && eType == EventType.ACQUIRE.ordinal()) {
                        depCopied.e1Acq = eDec;
                    }
                    depCopied.pin = false;
                    if(!depCopied.thrG1.get(eThr) && depCopied.exclThreadsBefG1.get(eThr)) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                    if(eType == EventType.JOIN.ordinal()) {
                        if(!depCopied.thrG1.get(eDec) && depCopied.exclThreadsBefG1.get(eDec)) {
                            depCopied.Frontier.update(eThr, eDec, eType);
                        }
                    }
                    depCopied.thrG1.set(eThr);
                    depCopied.exclThreadsBefG1.set(eThr);
                    if(eType == EventType.FORK.ordinal() || eType == EventType.JOIN.ordinal()) {
                        depCopied.exclThreadsBefG1.set(eDec);
                    }
                    if(eType == EventType.READ.ordinal() && !depCopied.wtG1.get(eDec)) {
                        if(depCopied.exclWtVarsBefG1.get(eDec)) {
                            depCopied.Frontier.update(eThr, eDec, eType);
                        }
                        else {
                            if(!depCopied.rdFrontier.containsKey(eDec)) {
                                depCopied.rdFrontier.put(eDec, new MazFrontier());
                            }
                            depCopied.rdFrontier.get(eDec).update(eThr, eDec, eType);
                        }
                    }
                     
                    if(eType == EventType.WRITE.ordinal()) {
                        depCopied.wtG1.set(eDec);
                        depCopied.exclWtVarsBefG1.set(eDec);
                    }
                    if(eType == EventType.ACQUIRE.ordinal()) {
                        if(depCopied.openLocks.get(eDec)) {
                            depCopied.Frontier.update(eThr, eDec, eType);
                        }
                        else {
                            if(!depCopied.acqFrontier.containsKey(eDec)) {
                                depCopied.acqFrontier.put(eDec, new MazFrontier());
                            }
                            depCopied.acqFrontier.get(eDec).update(eThr, eDec, eType);
                        }
                        depCopied.openLocksG1.set(eDec);
                    }
                    if(eType == EventType.RELEASE.ordinal()) {
                        depCopied.openLocksG1.clear(eDec);
                    }
                    if(depCopied.Frontier.isDependentWith(eThr, eDec, eType)) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                    if(eThr == depCopied.e1Thr || ((eType == EventType.WRITE.ordinal() || (eType == EventType.READ.ordinal() && depCopied.e1Write)) && depCopied.e1Var == eDec) || ((eType == EventType.FORK.ordinal() || eType == EventType.JOIN.ordinal()) && eDec == depCopied.e1Thr)) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                    for(int var: depCopied.rdFrontier.keySet()) {
                        MazFrontier fr = depCopied.rdFrontier.get(var);
                        if(fr.isDependentWith(eThr, eDec, eType)) {
                            fr.update(eThr, eDec, eType);
                        }
                    }
                    for(int lck: depCopied.acqFrontier.keySet()) {
                        MazFrontier fr = depCopied.acqFrontier.get(lck);
                        if(fr.isDependentWith(eThr, eDec, eType)) {
                            fr.update(eThr, eDec, eType);
                        }
                    }
                    addToStates13(newStates, newBirths, depCopied, birth, 1);
                }
            }
            }
            // choose as e1
            {
                if(dep.e1Thr == -1 && (eType == EventType.READ.ordinal() || eType == EventType.WRITE.ordinal()) && choose(eDec)) {
                    if((dep.thrG1.get(eThr) || !dep.exclThreadsBefG1.get(eThr)) && !dep.Frontier.isDependentWith(eThr, eDec, eType)) {
                        boolean flag = true;
                        DependentInfo depCopied = new DependentInfo(dep);
                        if(depCopied.ignore(eThr, eDec, eType, lockHold, 1)) {
                            depCopied.e1Thr = eThr;
                            depCopied.e1Var = eDec;
                            depCopied.e1Write = eType == EventType.WRITE.ordinal();
                            depCopied.thrG1.set(eThr);
                            if(depCopied.e1Write) {
                                depCopied.wtG1.set(eDec);
                            }
                            
                            for(int var: depCopied.rdFrontier.keySet()) {
                                MazFrontier fr = depCopied.rdFrontier.get(var);
                                if(fr.isDependentWith(eThr, eDec, eType)) {
                                    fr.isDependentWithE1 = true;
                                    fr.update(eThr, eDec, eType);
                                }
                            }
                            for(int lck: depCopied.acqFrontier.keySet()) {
                                MazFrontier fr = depCopied.acqFrontier.get(lck);
                                if(fr.isDependentWith(eThr, eDec, eType)) {
                                    fr.isDependentWithE1 = true;
                                    fr.update(eThr, eDec, eType);
                                }
                            } 
                            if(flag){
                                addToStates13(newStates, newBirths, depCopied, birth, 1);
                            }
                        }

                    }
                }
            }
            }
        }
        statesPhase1 = newStates;
        birthsPhase1 = newBirths;
    }

    private void updatePhase3(int eThr, int eDec, int eType) {
        HashMap<String, DependentInfo> newStates = new HashMap<>();
        PriorityQueue<Long> newBirths = new PriorityQueue<>();
        for(String hashString: statesPhase3.keySet()){
            stateSize += 1;
            DependentInfo dep = statesPhase3.get(hashString);
            long birth = dep.birth;
            if(LRU) {
                if(!dep.pin && !birthsPhase3.isEmpty() && birth < birthsPhase3.peek() && birth != 0) {
                    continue;
                }
            }
            // if(eventCount == 9 && dep.e1Write && dep.e1Var == 0 && dep.e1Thr == 0) {
            //     System.out.println(dep.toString());
            // }
            // else if(heuristic == 2) {
            //     if(timestamp - birth >= lifetime) {
            //         continue;
            //     }
            // }

            if(grainSize) {
                if(dep.G1Size + dep.G2Size > sizelimit) {
                    continue;
                }
            }

            if(grainPattern && !dep.thrG2.isEmpty() && !dep.thrG2.get(eThr)) {
                continue;
            }
            if(!grainPattern || eType == EventType.READ.ordinal()) {
                dep.G2Size += 1;
                if(dep.G2Size > 1) {
                    dep.pin = false;
                }
                DependentInfo depCopied = new DependentInfo(dep);  
                if(depCopied.checkLock(eThr, lockHold, 3)) {
                if(!depCopied.thrG2.get(eThr) && (depCopied.exclThreadsAftG1.get(eThr) || (!depCopied.inclThreadsAftG1.get(eThr) && depCopied.exclThreadsBefG1.get(eThr) && !depCopied.thrG1.get(eThr)))) {
                    depCopied.Frontier.update(eThr, eDec, eType);
                }
                if(eType == EventType.JOIN.ordinal()) {
                    if(!depCopied.thrG2.get(eDec) && (depCopied.exclThreadsAftG1.get(eDec) || (!depCopied.inclThreadsAftG1.get(eDec) && depCopied.exclThreadsBefG1.get(eDec) && !depCopied.thrG1.get(eDec)))) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                }
                depCopied.thrG2.set(eThr);
                depCopied.exclThreadsAftG1.set(eThr);
                if(eType == EventType.FORK.ordinal() || eType == EventType.JOIN.ordinal()) {
                    depCopied.exclThreadsAftG1.set(eDec);
                }
                if(eType == EventType.READ.ordinal() && !depCopied.wtG2.get(eDec)) {
                    if( depCopied.exclWtVarsAftG1.get(eDec) || 
                        (depCopied.inclWtVarsAftG1.get(eDec) && depCopied.wtG1.get(eDec))) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                }
                if(eType == EventType.WRITE.ordinal()) {
                    depCopied.wtG2.set(eDec);
                    depCopied.exclWtVarsAftG1.set(eDec);
                }
                if(eType == EventType.ACQUIRE.ordinal()) {
                    if(depCopied.openLocks.get(eDec) || depCopied.openLocksG1.get(eDec)) {
                        depCopied.Frontier.update(eThr, eDec, eType);
                    }
                }
                if(depCopied.Frontier.isDependentWith(eThr, eDec, eType)) {
                    depCopied.Frontier.update(eThr, eDec, eType);
                }
                if(eThr == depCopied.e1Thr || ((eType == EventType.WRITE.ordinal() || (eType == EventType.READ.ordinal() && depCopied.e1Write)) && depCopied.e1Var == eDec) || ((eType == EventType.FORK.ordinal() || eType == EventType.JOIN.ordinal()) && eDec == depCopied.e1Thr)) {
                    depCopied.Frontier.update(eThr, eDec, eType);
                }
                addToStates13(newStates, newBirths, depCopied, birth, 3);
            }
            }

            // choose as e2
            {
                if(eThr != dep.e1Thr && (eType == EventType.WRITE.ordinal() || (eType == EventType.READ.ordinal() && dep.e1Write)) && dep.e1Var == eDec) {
                    if((dep.thrG2.get(eThr) || (!dep.exclThreadsAftG1.get(eThr) && (dep.inclThreadsAftG1.get(eThr) || !dep.exclThreadsBefG1.get(eThr) || dep.thrG1.get(eThr)))) && !dep.Frontier.isDependentWith(eThr, eDec, eType)) {
                        DependentInfo depCopied = new DependentInfo(dep); 
                        if (depCopied.checkLock(eThr, lockHold, 3)) {
                            // System.out.println(eventCount + " " + eThr + " " + eDec + " " + depCopied);
                            racy = true;
                        }   
                    }
                }
            } 
        }
        statesPhase3 = newStates;
        birthsPhase3 = newBirths;
    }

    private void addToStates13(HashMap<String, DependentInfo> states, PriorityQueue<Long> births, DependentInfo dep, long birth, int phase) {
        BitSet exlthreadsTotal = (BitSet)dep.exclThreadsBefG1.clone();
        exlthreadsTotal.or(dep.exclThreadsAftG1);
        if(phase == 1 && exlthreadsTotal.nextClearBit(0) == numOfThreads) {
            return;
        }
        dep.birth = birth;
        dep.hashString = dep.toString();
        if(states.containsKey(dep.hashString)) {
            DependentInfo depDup = states.get(dep.hashString);
            if(depDup.birth < birth) {
                if(LRU) {
                    births.remove(depDup.birth);
                    births.add(birth);
                }
                depDup.birth = birth;
            }
            return;
        }
        if(phase == 1 || phase == 3) {
            for(Iterator<Entry<String, DependentInfo>> it = states.entrySet().iterator(); it.hasNext();) {
                DependentInfo depInfo = it.next().getValue();
                if((phase == 0 && depInfo.subsumePhase0(dep, lockHold2)) || (phase == 2 && depInfo.subsumePhase2(dep, lockHold2))) {
                    return;
                }
                if((phase == 0 && depInfo.subsumePhase0(dep, lockHold2)) || (phase == 2 && depInfo.subsumePhase2(dep, lockHold2))) {
                    it.remove();
                    births.remove(depInfo.birth);
                }
            }
        }
        
        if(LRU) {
            if(birth != 0) {
                if(births.size() >= poolSize) {
                    if(births.peek() < birth) {
                        births.poll();
                        births.add(birth);
                        states.put(dep.hashString, dep);
                    }
                }
                else {
                    births.add(birth);
                    states.put(dep.hashString, dep);
                }
            }
            else {
                states.put(dep.hashString, dep);
            }
        }
        
    }

    private void addToStates02(HashMap<Integer, HashMap<String, DependentInfo>> states, PriorityQueue<Long> births, DependentInfo dep, long birth, int phase) {
        BitSet exlthreadsTotal = (BitSet)dep.exclThreadsBefG1.clone();
        exlthreadsTotal.or(dep.exclThreadsAftG1);
        if(exlthreadsTotal.nextClearBit(0) == numOfThreads) {
            return;
        }
        dep.birth = birth;
        dep.hashString = dep.toString();
        if(!states.containsKey(dep.e1Var)) {
            states.put(dep.e1Var, new HashMap<>());
        }
        HashMap<String, DependentInfo> substates = states.get(dep.e1Var);
        if(substates.containsKey(dep.hashString)) {
            DependentInfo depDup = substates.get(dep.hashString);
            if(depDup.birth < birth) {
                if(LRU) {
                    births.remove(depDup.birth);
                    births.add(birth);
                }
                depDup.birth = birth;
            }
            return;
        }
        if(phase == 0 || phase == 2) {
            for(Iterator<Entry<String, DependentInfo>> it = substates.entrySet().iterator(); it.hasNext();) {
                DependentInfo depInfo = it.next().getValue();
                if((phase == 0 && depInfo.subsumePhase0(dep, lockHold2)) || (phase == 2 && depInfo.subsumePhase2(dep, lockHold2))) {
                    return;
                }
                if((phase == 0 && depInfo.subsumePhase0(dep, lockHold2)) || (phase == 2 && depInfo.subsumePhase2(dep, lockHold2))) {
                    it.remove();
                    births.remove(depInfo.birth);
                }
            }
        }
        if(LRU) {
            if(birth != 0) {
                if(births.size() >= poolSize) {
                    if(births.peek() < birth) {
                        births.poll();
                        births.add(birth);
                        substates.put(dep.hashString, dep);
                    }
                }
                else {
                    births.add(birth);
                    substates.put(dep.hashString, dep);
                }
            }
            else {
                substates.put(dep.hashString, dep);
            }
        }
        
    }

    public void printMemory() {
    }
}

class DependentInfo {
    public BitSet exclThreadsBefG1;
    public BitSet exclThreadsAftG1;
    public BitSet inclThreadsAftG1;
    public BitSet exclWtVarsBefG1;
    public BitSet exclWtVarsAftG1;
    public BitSet inclWtVarsAftG1;
    public BitSet openLocks;

    public int e1Var;
    public int e1Thr;
    public boolean e1Write;
    public int e1Acq;

    public BitSet thrG1;
    public BitSet wtG1;
    public BitSet openLocksG1;

    public BitSet thrG2;
    public BitSet wtG2;

    public MazFrontier Frontier;
    public HashMap<Integer, MazFrontier> rdFrontier;
    public HashMap<Integer, MazFrontier> acqFrontier;
    public int G1Size;
    public int G2Size;
    // public int G1Time;
    // public int activeAcq;

    public long birth;
    public boolean pin;

    public String hashString;

    public DependentInfo() {
        exclThreadsBefG1 = new BitSet(State.numOfThreads);
        exclThreadsAftG1 = new BitSet(State.numOfThreads);
        inclThreadsAftG1 = new BitSet(State.numOfThreads);
        exclWtVarsBefG1 = new BitSet(State.numOfVariables); 
        exclWtVarsAftG1 = new BitSet(State.numOfVariables);
        inclWtVarsAftG1 = new BitSet(State.numOfVariables);
        openLocks = new BitSet(State.numOfLocks); 
        e1Var = -1;
        e1Thr = -1;
        pin = false;
        e1Write = false;
        e1Acq = -1;
        thrG1 = new BitSet(State.numOfThreads);
        wtG1 = new BitSet(State.numOfVariables);
        openLocksG1 = new BitSet(State.numOfLocks);
        thrG2 = new BitSet(State.numOfThreads);
        wtG2 = new BitSet(State.numOfVariables); 
        Frontier = new MazFrontier();
        rdFrontier = new HashMap<>();
        acqFrontier = new HashMap<>();
        G1Size = 0;
        G2Size = 0;
        // G1Time = 0;
        // activeAcq = 0;
        birth = 0;
        hashString = toString();
    }

    public DependentInfo(DependentInfo other) {
        exclThreadsBefG1 = (BitSet)other.exclThreadsBefG1.clone();
        exclThreadsAftG1 = (BitSet)other.exclThreadsAftG1.clone();
        inclThreadsAftG1 = (BitSet)other.inclThreadsAftG1.clone();
        exclWtVarsBefG1 = (BitSet)other.exclWtVarsBefG1.clone(); 
        exclWtVarsAftG1 = (BitSet)other.exclWtVarsAftG1.clone();
        inclWtVarsAftG1 = (BitSet)other.inclWtVarsAftG1.clone();
        openLocks = (BitSet)other.openLocks.clone(); 
        e1Var = other.e1Var;
        e1Thr = other.e1Thr;
        e1Write = other.e1Write;
        e1Acq = other.e1Acq;
        pin = other.pin;
        thrG1 = (BitSet)other.thrG1.clone();
        wtG1 = (BitSet)other.wtG1.clone();
        openLocksG1 = (BitSet)other.openLocksG1.clone();
        thrG2 = (BitSet)other.thrG2.clone();
        wtG2 = (BitSet)other.wtG2.clone(); 
        Frontier = new MazFrontier(other.Frontier);
        if(other.rdFrontier == null) {
            rdFrontier = null;
        }
        else {
            rdFrontier = new HashMap<>();
            for(int l: other.rdFrontier.keySet()) {
                rdFrontier.put(l, new MazFrontier(other.rdFrontier.get(l)));
            }
        }
        if(other.acqFrontier == null) {
            acqFrontier = null;
        }
        else {
            acqFrontier = new HashMap<>();
            for(int l: other.acqFrontier.keySet()) {
                acqFrontier.put(l, new MazFrontier(other.acqFrontier.get(l)));
            }
        }
        this.G1Size = other.G1Size;
        this.G2Size = other.G2Size;
        // this.G1Time = other.G1Time;
        // this.activeAcq = other.activeAcq;
        birth = other.birth;
        hashString = toString();
    }


    public boolean mustIgnore(int eThr, int eDec, int eType){
        boolean ignoreBefG1 = (exclThreadsBefG1.get(eThr)) ||
            (eType == EventType.ACQUIRE.ordinal() && openLocks.get(eDec)) ||
            (eType == EventType.READ.ordinal() && exclWtVarsBefG1.get(eDec) && !inclWtVarsAftG1.get(eDec)) ||
            (eType == EventType.JOIN.ordinal() && exclThreadsBefG1.get(eDec));
        boolean ignoreAftG1 = (exclThreadsAftG1.get(eThr)) ||
            (eType == EventType.ACQUIRE.ordinal() && openLocks.get(eDec)) ||
            (eType == EventType.READ.ordinal() && exclWtVarsAftG1.get(eDec)) ||
            (eType == EventType.JOIN.ordinal() && exclThreadsAftG1.get(eDec));
        return ignoreBefG1 || ignoreAftG1;
	}

    public boolean checkLock(int eThr, HashMap<Integer, HashMap<Integer, Integer>> lockHold, int phase) {
        if( (phase == 0 && !exclThreadsBefG1.get(eThr)) ||
        (phase == 1 && !thrG1.get(eThr) && !exclThreadsBefG1.get(eThr)) ||
        (phase == 2 && !exclThreadsAftG1.get(eThr) && !exclThreadsBefG1.get(eThr)) ||
        (phase == 3 && !thrG2.get(eThr) && !exclThreadsAftG1.get(eThr) && !exclThreadsBefG1.get(eThr))) {
            for(int lck: lockHold.get(eThr).keySet()) {
                openLocks.set(lck);
                if(acqFrontier != null && acqFrontier.containsKey(lck)) {
                    MazFrontier fr = acqFrontier.get(lck);
                    if(fr.isDependentWithE1) {
                        return false;
                    }
                    else {
                        Frontier.union(fr);
                    }
                }
            }
        }
        return true;
    }

    public boolean ignore(int eThr, int eDec, int eType, HashMap<Integer, HashMap<Integer, Integer>> lockHold, int phase) {
        if(!checkLock(eThr, lockHold, phase)) {
            return false;
        }

        if (phase == 0 || phase == 1) {
            exclThreadsBefG1.set(eThr);
        }
        else {
            exclThreadsAftG1.set(eThr);
        }
        if(eType == EventType.WRITE.ordinal()) {
            if(phase == 0 || phase == 1) {
                exclWtVarsBefG1.set(eDec);
            }
            else {
                exclWtVarsAftG1.set(eDec);
            }
        }
        if(eType == EventType.FORK.ordinal() || eType == EventType.JOIN.ordinal()) {
            if (phase == 0 || phase == 1) {
                exclThreadsBefG1.set(eDec);
            }
            else {
                exclThreadsAftG1.set(eDec);
            }
        }
        return true;
    }

    private boolean subsume(BitSet b1, BitSet b2) {
        BitSet b1Clone = (BitSet)b1.clone();
        b1Clone.andNot(b2);
        return b1Clone.isEmpty();
    }    

    private boolean subsumeOpenLock(BitSet t1, BitSet t2, BitSet l2, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {
        BitSet t1Clone = (BitSet)t1.clone();
        t1Clone.andNot(t2);
        for(int i = t1Clone.nextSetBit(0); i >= 0; i = t1Clone.nextSetBit(i+1)) {
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

    public boolean subsumePhase0(DependentInfo other, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {

        return  subsume(this.exclThreadsBefG1, other.exclThreadsBefG1) &&
                subsume(this.exclWtVarsBefG1, other.exclWtVarsBefG1) &&
                subsume(this.openLocks, other.openLocks) && 
                subsumeOpenLock(other.exclThreadsBefG1, this.exclThreadsBefG1, other.openLocks, lockHold);         
    }

    public boolean subsumePhase2(DependentInfo other, HashMap<Integer, HashMap<Integer, Integer>> lockHold) {
        BitSet thisCombineExclThreads = (BitSet)this.exclThreadsBefG1.clone();
        thisCombineExclThreads.or(this.exclThreadsAftG1);
        BitSet otherCombineExclThreads = (BitSet)other.exclThreadsBefG1.clone();
        otherCombineExclThreads.or(other.exclThreadsAftG1);

        BitSet thisCombineExclWtVars = (BitSet)this.exclWtVarsBefG1.clone();
        thisCombineExclWtVars.andNot(this.inclWtVarsAftG1);
        thisCombineExclWtVars.or(this.exclWtVarsAftG1);
        BitSet otherCombineExclWtVars = (BitSet)other.exclWtVarsBefG1.clone();
        otherCombineExclWtVars.andNot(other.inclWtVarsAftG1);
        otherCombineExclWtVars.or(other.exclWtVarsAftG1); 
        return  this.pin &&
                this.e1Var == other.e1Var &&
                (this.e1Write || !other.e1Write) && 
                subsume(thisCombineExclThreads, otherCombineExclThreads) &&
                subsume(thisCombineExclWtVars, otherCombineExclWtVars) &&
                subsume(this.openLocks, other.openLocks) && 
                subsumeOpenLock(otherCombineExclThreads, thisCombineExclThreads, other.openLocks, lockHold);         
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(exclThreadsBefG1);
        sb.append(exclThreadsAftG1);
        sb.append(inclThreadsAftG1);
        sb.append(exclWtVarsBefG1);
        sb.append(exclWtVarsAftG1);
        sb.append(inclWtVarsAftG1);
        sb.append(openLocks);

        sb.append(e1Thr);
        sb.append(e1Var);
        sb.append(e1Write);
        sb.append(e1Acq);
        sb.append(pin);

        sb.append(thrG1);
        sb.append(wtG1);
        sb.append(openLocksG1);

        sb.append(thrG2);
        sb.append(wtG2);

        sb.append(Frontier);
        sb.append(rdFrontier);
        sb.append(acqFrontier);
        sb.append(G1Size);
        sb.append(G2Size);
        // sb.append(G1Time);
        // sb.append(activeAcq);
        return sb.toString();
    }

}

class MazFrontier {
    public BitSet threads;
    public BitSet rdVars;
    public BitSet wtVars;
    public BitSet locks;
    public boolean isDependentWithE1;

    public MazFrontier() {
        threads = new BitSet(State.numOfThreads);
        rdVars = new BitSet(State.numOfVariables);
        wtVars = new BitSet(State.numOfVariables); 
        locks = new BitSet(State.numOfLocks);
        isDependentWithE1 = false;
    }

    public MazFrontier(MazFrontier other) {
        threads = (BitSet)other.threads.clone();
        rdVars = (BitSet)other.rdVars.clone();
        wtVars = (BitSet)other.wtVars.clone();
        locks = (BitSet)other.locks.clone();
        isDependentWithE1 = other.isDependentWithE1;
    }

    public boolean isDependentWith(int eThr, int eDec, int eType) {
        if(threads.get(eThr)) {
            return true;
        }
        if((eType == EventType.JOIN.ordinal() || eType == EventType.FORK.ordinal()) && threads.get(eDec)) {
            return true;
        }
        if(eType == EventType.READ.ordinal() && wtVars.get(eDec)) {
            return true;
        }
        if(eType == EventType.WRITE.ordinal() && (wtVars.get(eDec) || rdVars.get(eDec))) {
            return true;
        }
        if((eType == EventType.ACQUIRE.ordinal() || eType == EventType.RELEASE.ordinal()) && locks.get(eDec)) {
            return true;
        }
        return false;
    }

    public void update(int eThr, int eDec, int eType) {
        threads.set(eThr);
        if(eType == EventType.JOIN.ordinal() || eType == EventType.FORK.ordinal()) {
            threads.set(eDec);
        }
        if(eType == EventType.READ.ordinal()) {
            rdVars.set(eDec);
        }
        if(eType == EventType.WRITE.ordinal()) {
            wtVars.set(eDec);
        }
        if(eType == EventType.ACQUIRE.ordinal() || eType == EventType.RELEASE.ordinal()) {
            locks.set(eDec);
        }
    }

    public void union(MazFrontier other) {
        threads.or(other.threads);
        rdVars.or(other.rdVars);
        wtVars.or(other.wtVars);
        locks.or(other.locks);
    }

    private boolean subsume(BitSet b1, BitSet b2) {
        BitSet b1Clone = (BitSet)b1.clone();
        b1Clone.andNot(b2);
        return b1Clone.isEmpty();
    }

    public boolean subsume(MazFrontier other) {
        return !this.threads.isEmpty() && !other.threads.isEmpty() && subsume(this.threads, other.threads) && subsume(this.wtVars, other.wtVars) && subsume(this.rdVars, other.rdVars) && subsume(this.locks, other.locks) && this.isDependentWithE1 == other.isDependentWithE1;
    }

    public void toString(StringBuffer sb) {
        sb.append(threads);
        sb.append(wtVars);
        sb.append(rdVars);
        sb.append(locks);
        sb.append(isDependentWithE1);
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(threads);
        sb.append(wtVars);
        sb.append(rdVars);
        sb.append(locks);
        sb.append(isDependentWithE1);
        return sb.toString();
    }
}

class DependentInfoComparator implements Comparator<DependentInfo> {
    public int compare(DependentInfo o1, DependentInfo o2) {
        return o1.hashString.compareTo(o2.hashString);
    }
}
