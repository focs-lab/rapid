package engine.racedetectionengine.seqpreserving.preprocess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import event.EventType;

public class State {

    private HashMap<Integer, HashSet<Integer>> protectedLocks = new HashMap<>(); 
    private HashMap<Integer, HashMap<Integer, Integer>> lockHold = new HashMap<>();

    public boolean update(PreprocessEvent e) {
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
        if(!lockHold.containsKey(eThr)) {
			lockHold.put(eThr, new HashMap<>());
		}
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
        if(eType == EventType.WRITE.ordinal() || eType == EventType.READ.ordinal()) {
            if(!protectedLocks.containsKey(eDec)) {
                protectedLocks.put(eDec, new HashSet<>(lockHold.get(eThr).keySet()));
            }
            else {
                protectedLocks.get(eDec).retainAll(lockHold.get(eThr).keySet());
            }
        }
        return true;
    }

    public HashSet<Integer> getProtectedVars() {
        return protectedLocks.keySet().stream().filter((x) -> !protectedLocks.get(x).isEmpty()).collect(Collectors.toCollection(HashSet::new));
    }

    public void printMemory() {
    }
}