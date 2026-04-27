package engine.pattern.ConfPreservingPrefix;

import java.util.ArrayList;
import java.util.Iterator;

import util.Pair;

import engine.pattern.PatternTrack.VectorClockEvent;
import engine.pattern.PatternTrack.VectorClockState;
import util.PipedDeepCopy;

public class PrefixEvent extends VectorClockEvent {

    public boolean Handle(PrefixState state) {
        ArrayList<Pair<VectorClockState, DependentInfo>> newStates = new ArrayList<>();
        boolean matched = false;
        
        boolean threadLocal = this.getType().isTransactionType();
        for(Iterator<Pair<VectorClockState, DependentInfo>> iterator = state.states.iterator(); iterator.hasNext();){
            Pair<VectorClockState, DependentInfo> track_state = iterator.next(); 
            if(!threadLocal && mustIgnore(track_state.second)) {
                ignore(track_state.second);
                if(track_state.second.allThreads(state.tSet.size())) {
                    iterator.remove();
                }
            }
            else {
                if(!threadLocal /*&& !this.type.isRelease() && !this.type.isRead()*/) {
                    DependentInfo dep_new = (DependentInfo) PipedDeepCopy.copy(track_state.second);
                    ignore(dep_new);
                    if(!dep_new.allThreads(state.tSet.size())) {
                        VectorClockState copied_state = (VectorClockState) PipedDeepCopy.copy(track_state.first);
                        newStates.add(new Pair<VectorClockState,DependentInfo>(copied_state, dep_new));
                    }
                }

                matched = super.Handle(track_state.first);
                if(matched) {
                    return true;
                }
                if(this.getType().isWrite()) {
                    track_state.second.remove(this.variable);
                }
                if(this.getType().isAcquire()) {
                    track_state.second.add(this.lock);
                }
                if(this.getType().isRelease()) {
                    track_state.second.remove(this.lock);
                }
            }
        }
        state.states.addAll(newStates);
		return matched;
	}

    boolean mustIgnore(DependentInfo dep){

        if(this.getType().isAcquire()) {
            return dep.check_dependency(this.thread) || dep.check_dependency(this.lock);
        }

        if (this.getType().isRead()) {
            return dep.check_dependency(this.thread) || dep.check_dependency(this.variable);
        }

        if(this.getType().isJoin()) {
            return dep.check_dependency(this.thread) || dep.check_dependency(this.target);
        }

		return dep.check_dependency(this.thread);
	}

    void ignore(DependentInfo dep) {
        dep.add(this.thread);

        if(this.getType().isWrite()) {
            dep.add(this.variable);
        }

        if(this.getType().isFork()) {
            dep.add(this.target);
        }
    }
}
