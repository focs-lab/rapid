package util.vectorclock;

import java.util.Vector;

public class VectorClockOpt {

	private int dim;
	private Vector<Integer> clock;

	public VectorClockOpt(int d) {
		this.dim = d;
		this.clock = new Vector<Integer>(dim);
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.addElement(0);
		}
	}

	public VectorClockOpt(VectorClockOpt fromVectorClock) {
		this.dim = fromVectorClock.getDim();
		this.clock = new Vector<Integer>(dim);
		Vector<Integer> fromClock = fromVectorClock.getClock();
		for (int ind = 0; ind < fromVectorClock.getDim(); ind++) {
			this.clock.addElement((Integer) fromClock.get(ind));
		}
	}

	public int getDim() {
		return this.dim;
	}

	public Vector<Integer> getClock() {
		return this.clock;
	}

	public String toString() {
		return this.clock.toString();
	}

	public boolean isZero() {
		boolean itIsZero = true;
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			if (thisVal != 0) {
				itIsZero = false;
				break;
			}
		}
		return itIsZero;
	}

	public boolean isEqual(VectorClockOpt vc) {
		boolean itIsEqual = true;
		Vector<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int vcVal = vcClock.get(ind).intValue();
			// System.out.println("Comparing: " + thisVal + " | " + vcVal);
			if (thisVal != vcVal) {
				itIsEqual = false;
				break;
			}
		}
		return itIsEqual;
	}

	public boolean isLessThan(VectorClockOpt vc) {
		boolean OneComponentIsLess = false;
		boolean isLessThanOrEqual = true;
		Vector<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int vcVal = vcClock.get(ind).intValue();
			if (thisVal > vcVal) {
				isLessThanOrEqual = false;
				break;
			}
			else if(thisVal < vcVal) {
				OneComponentIsLess = true;
			}
		}
		return OneComponentIsLess && isLessThanOrEqual;
	}
	
	public boolean isLessThanOrEqual(VectorClockOpt vc) {
		boolean itIsLessThanOrEqual = true;
		Vector<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int vcVal = vcClock.get(ind).intValue();
			if (!(thisVal <= vcVal)) {
				itIsLessThanOrEqual = false;
				break;
			}
		}
		return itIsLessThanOrEqual;
	}
	
	public boolean isLessThanOrEqual(VectorClockOpt vc, int ind) {
		return this.clock.get(ind).intValue() <= vc.getClock().get(ind).intValue();
	}

	public void setToZero() {
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, (Integer) 0 );
		}
	}
	
	public void copyFrom(VectorClockOpt vc) {
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, (Integer) vc.clock.get(ind));
		}
	}	
	
	private void updateMax2(VectorClockOpt vc) {
		for (int ind = 0; ind < this.dim; ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind);
			int max_c = this_c > vc_c ? this_c : vc_c;
			this.clock.set(ind, (Integer) max_c);
		}
	}
	
	//The following function update this as : this := \lambda t . if t == tIndex then this[tIndex] else max(this[t], vc[t])
	public void updateMax2WithoutLocal(VectorClockOpt vc, int tIndex) {
		for (int ind = 0; ind < this.dim; ind++) {
			if(ind != tIndex){
				int this_c = this.clock.get(ind);
				int vc_c = vc.clock.get(ind);
				int max_c = this_c > vc_c ? this_c : vc_c;
				this.clock.set(ind, (Integer) max_c);
			}
		}
	}
	
	public void updateWithMax(VectorClockOpt... vcList) {
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			VectorClockOpt vc = vcList[i];
			this.updateMax2(vc);
		}
	}
	
	private void updateMin2(VectorClockOpt vc) {
		for (int ind = 0; ind < this.dim; ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind);
			int max_c = this_c < vc_c ? this_c : vc_c;
			this.clock.set(ind, (Integer) max_c);
		}
	}
	
	public void updateWithMin(VectorClockOpt... vcList) {
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			VectorClockOpt vc = vcList[i];
			this.updateMin2(vc);
		}
	}
	
	public int getClockIndex(int tIndex){
		return this.clock.get(tIndex);
	}
	
	public void setClockIndex(int tIndex, int tValue){
		this.clock.set(tIndex, (Integer) tValue);
	}
	
}