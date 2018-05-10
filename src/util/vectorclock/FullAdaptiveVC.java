package util.vectorclock;

public class FullAdaptiveVC extends AdaptiveVC{
	
	public FullAdaptiveVC() {
		super();
	}

//	public FullAdaptiveVC(int dim) {
//		super(dim);
//	}

	@Override
	public boolean isLTEUpdateWithMax(VectorClock vc, int t){
		boolean isLTE = isLessThanOrEqual(vc);
		if(isLTE){
			this.is_epoch = true;
			this.vc = null;
			this.epoch.setClock(vc.getClockIndex(t));
			this.epoch.setThreadIndex(t);
		}
		else{
			if(is_epoch){
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
			}
			this.vc.setClockIndex(t, vc.getClockIndex(t));
		}
		return isLTE;
	}
}