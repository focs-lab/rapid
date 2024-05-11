package util.vectorclock;

public class SemiAdaptiveVC extends AdaptiveVC{
	
	public SemiAdaptiveVC() {
		super();
	}

//	public SemiAdaptiveVC(int dim) {
//		super(dim);
//	}

	@Override
	public boolean isLTEUpdateWithMax(VectorClock vc, int t){
		boolean isLTE = isLessThanOrEqual(vc);
		if(is_epoch){
			if(isLTE){
				this.epoch.setClock(vc.getClockIndex(t));
				this.epoch.setThreadIndex(t);
			}
			else{
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
				this.vc.setClockIndex(t, vc.getClockIndex(t));
			}
		}
		else{
			this.vc.setClockIndex(t, vc.getClockIndex(t));
		}
		return isLTE;
	}
	
	public void updateWithMax(VectorClock vc, int t){
		boolean isLTE = isLessThanOrEqual(vc);
		if(is_epoch){
			if(isLTE){
				this.epoch.setClock(vc.getClockIndex(t));
				this.epoch.setThreadIndex(t);
			}
			else{
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
				this.vc.setClockIndex(t, vc.getClockIndex(t));
			}
		}
		else{
			this.vc.setClockIndex(t, vc.getClockIndex(t));
		}
	}
}