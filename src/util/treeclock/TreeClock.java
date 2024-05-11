package util.treeclock;

//A TreeClock implementation using 2 long[] arrays and the stack is an array


import java.util.Arrays;
import util.PairHardCodeWordTricks;
import util.NeibhorsHardCodeWordTricks;


public class TreeClock {

	public int dim;
	public short rootTid = -1;	
	public long[] clocks, tree;
	public short[] S;
	public int top;


	public TreeClock(int dim) {
		this.dim = dim;
		this.clocks = new long[dim];
		this.tree = new long[dim];
		this.S = new short[dim];
		this.top = -1;

		for(int i=0; i<this.dim; i++) {
			this.clocks[i] = (long) 0;
			this.tree[i] = NeibhorsHardCodeWordTricks.NULL;
		}
	}

	public TreeClock(short tid, int dim) {
		this.dim = dim;
		this.rootTid = tid;
		this.clocks = new long[dim];
		this.tree = new long[dim];
		this.S = new short[dim];
		this.top = -1;

		for(int i=0; i<this.dim; i++) {
			this.clocks[i] = (long) 0;
			this.tree[i] = NeibhorsHardCodeWordTricks.NULL;
		}

	}

	public TreeClock(short tid, int tval, int dim) {
		this.dim = dim;
		this.rootTid = tid;
		this.clocks = new long[dim];
		this.tree = new long[dim];
		this.S = new short[dim];
		this.top = -1;

		for(int i=0; i<this.dim; i++) {
			this.clocks[i] = (long) 0;
			this.tree[i] = NeibhorsHardCodeWordTricks.NULL;
		}

		this.clocks[tid] = (long) tval;
	}

	public TreeClock(TreeClock fromTreeClock) {
		this.rootTid = fromTreeClock.rootTid;
		this.clocks = Arrays.copyOf(fromTreeClock.clocks, dim);
		this.tree = Arrays.copyOf(fromTreeClock.tree, dim);
		this.S = Arrays.copyOf(fromTreeClock.S, dim);
		this.top = -1;
	}

	public void deepCopy(TreeClock fromTreeClock) {
		this.rootTid = fromTreeClock.rootTid;
		this.clocks = Arrays.copyOf(fromTreeClock.clocks, dim);
		this.tree = Arrays.copyOf(fromTreeClock.tree, dim);
		this.S = Arrays.copyOf(fromTreeClock.S, dim);
		this.top = -1;
	}

	private void detachFromNeighbors(short t, long node) {
		short t_next = NeibhorsHardCodeWordTricks.getNext(node);
		short t_prev = NeibhorsHardCodeWordTricks.getPrevious(node);
		short t_parent = NeibhorsHardCodeWordTricks.getParent(node);
		long parent_node = this.tree[t_parent];

		if(NeibhorsHardCodeWordTricks.getHeadChild(parent_node) == t) {
			this.tree[t_parent] =  NeibhorsHardCodeWordTricks.setHeadChild(t_next, parent_node);
		}
		else {
			this.tree[t_prev] = NeibhorsHardCodeWordTricks.setNext(t_next, this.tree[t_prev]);
		}
		if(t_next >= 0) {
			this.tree[t_next] = NeibhorsHardCodeWordTricks.setPrevious(t_prev, this.tree[t_next]);
		}

	}

	public int getLocalClock(short tid) { 
		if ( this.rootTid >= 0) {
			return PairHardCodeWordTricks.getClock(this.clocks[tid]);
		}
		return 0;
	}

	public Long getLocalRootData() {
		return this.clocks[this.rootTid];
	}

	public void incrementBy(int val) {
		this.clocks[this.rootTid] = PairHardCodeWordTricks.incrementClockBy(val, this.clocks[rootTid]);
	}

	public boolean isLessThanOrEqual(TreeClock tc) {
		if(this.rootTid < 0) {
			return true;
		}
		return PairHardCodeWordTricks.getClock(this.clocks[rootTid]) <= tc.getLocalClock(this.rootTid); 

	}
	
	public void join(TreeClock tc) {

		if(this.rootTid == tc.rootTid || tc.rootTid < 0){
			return;
		}

		short zprime_tid = tc.rootTid;
		long zprime_clocks = tc.getLocalRootData();
		int zprime_clock = PairHardCodeWordTricks.getClock(zprime_clocks);

		long z_node = this.tree[zprime_tid];
		long z_clocks = this.clocks[zprime_tid];
		int z_clock = 0;
		if(!NeibhorsHardCodeWordTricks.isNull(z_node)) {
			z_clock = PairHardCodeWordTricks.getClock(z_clocks);
			if (zprime_clock <= z_clock) {
				return;
			}
			else {
				this.detachFromNeighbors(zprime_tid, z_node);
			}
		}

		z_clocks = PairHardCodeWordTricks.copyClockToPclock(this.clocks[this.rootTid], zprime_clocks);
		long thisRootNode = this.tree[this.rootTid];


		short root_head_child = NeibhorsHardCodeWordTricks.T_NULL;
		if(!NeibhorsHardCodeWordTricks.isHeadChildNull(thisRootNode)) {
			root_head_child = NeibhorsHardCodeWordTricks.getHeadChild(thisRootNode);
			this.tree[root_head_child] = NeibhorsHardCodeWordTricks.setPrevious(zprime_tid, this.tree[root_head_child]);
		}

		z_node = NeibhorsHardCodeWordTricks.setNextAndParent(root_head_child, this.rootTid, z_node);
		this.clocks[zprime_tid] = z_clocks;
		this.tree[zprime_tid] = z_node;
		this.tree[this.rootTid] = NeibhorsHardCodeWordTricks.setHeadChild(zprime_tid, thisRootNode);


		short vprime_tid = NeibhorsHardCodeWordTricks.getHeadChild(tc.tree[zprime_tid]);
		while(!NeibhorsHardCodeWordTricks.isTNull(vprime_tid)) {
			long vprime_clocks = tc.clocks[vprime_tid];
			int v_clock = this.getLocalClock(vprime_tid);
			if(v_clock < PairHardCodeWordTricks.getClock(vprime_clocks)) {
				this.S[++this.top] = vprime_tid;
			}
			else {
				if(PairHardCodeWordTricks.getPclock(vprime_clocks) <= z_clock) {
					break;
				}
			}
			vprime_tid = NeibhorsHardCodeWordTricks.getNext(tc.tree[vprime_tid]);
		}

		while(top >=0 ) {			
			short uprime_tid = this.S[this.top--];
			long uprime_clocks = tc.clocks[uprime_tid];
			long u_node = this.tree[uprime_tid];
			long u_clocks = this.clocks[uprime_tid];
			int u_clock = 0;
			if(!NeibhorsHardCodeWordTricks.isNull(u_node)) {
				u_clock = PairHardCodeWordTricks.getClock(u_clocks);
				this.detachFromNeighbors(uprime_tid, u_node);
			}
			this.clocks[uprime_tid] = uprime_clocks;
			short y = NeibhorsHardCodeWordTricks.getParent(tc.tree[uprime_tid]);
			long yNode = this.tree[y];
			short head_child = NeibhorsHardCodeWordTricks.getHeadChild(yNode);
			if(!NeibhorsHardCodeWordTricks.isHeadChildNull(yNode)) {
				this.tree[head_child] = NeibhorsHardCodeWordTricks.setPrevious(uprime_tid, this.tree[head_child]);
			}
			u_node = NeibhorsHardCodeWordTricks.setNextAndParent(head_child, y, u_node);
			this.tree[uprime_tid] = u_node;
			this.tree[y] = NeibhorsHardCodeWordTricks.setHeadChild(uprime_tid, yNode);

			vprime_tid = NeibhorsHardCodeWordTricks.getHeadChild(tc.tree[uprime_tid]);
			while(!NeibhorsHardCodeWordTricks.isTNull(vprime_tid)) {
				long vprime_clocks = tc.clocks[vprime_tid];
				int v_clock = this.getLocalClock(vprime_tid);
				if(v_clock < PairHardCodeWordTricks.getClock(vprime_clocks)) {
					this.S[++this.top] = vprime_tid;	
				}
				else {
					if(PairHardCodeWordTricks.getPclock(vprime_clocks) <= u_clock) {
						break;
					}
				}
				vprime_tid = NeibhorsHardCodeWordTricks.getNext(tc.tree[vprime_tid]);
			} 
		}		
	}

	// Assumes tc.root is not null
	// Returns true if copy was monotone
	public boolean monotoneCopy(TreeClock tc) {
		if (this.rootTid < 0) {
			this.deepCopy(tc);
			return false;
		}
		
		short zprime_tid = tc.rootTid;
		long zprime_clocks = tc.getLocalRootData();

		long z_node = this.tree[zprime_tid];
		long z_clocks = this.clocks[zprime_tid];
		int z_clock = 0;

		if(!NeibhorsHardCodeWordTricks.isNull(z_node)) {
			z_clock = PairHardCodeWordTricks.getClock(z_clocks);
			if(zprime_tid != this.rootTid) {
				this.detachFromNeighbors(zprime_tid, z_node);
			}
		}
		
		z_clocks = zprime_clocks;
		z_node = NeibhorsHardCodeWordTricks.setParentNull(z_node);
		this.clocks[zprime_tid] = z_clocks;
		this.tree[zprime_tid] = z_node;

		short vprime_tid = NeibhorsHardCodeWordTricks.getHeadChild(tc.tree[zprime_tid]);
		while(!NeibhorsHardCodeWordTricks.isTNull(vprime_tid)) {
			long vprime_clocks = tc.clocks[vprime_tid];
			int v_clock = this.getLocalClock(vprime_tid);
			if(v_clock < PairHardCodeWordTricks.getClock(vprime_clocks)) {
					this.S[++this.top] = vprime_tid;
			}
			else {
				if(vprime_tid == this.rootTid) {
						this.S[++this.top] = vprime_tid;
				}
				if(PairHardCodeWordTricks.getPclock(vprime_clocks) <= z_clock) {
					break;
				}
			}
			
			vprime_tid = NeibhorsHardCodeWordTricks.getNext(tc.tree[vprime_tid]);
		}


		while(this.top >= 0) {
			short uprime_tid = this.S[this.top--];
			long uprime_clocks = tc.clocks[uprime_tid];
			long u_node = this.tree[uprime_tid];
			long u_clocks = this.clocks[uprime_tid];
			int u_clock = 0;
			if(uprime_tid !=this.rootTid && !NeibhorsHardCodeWordTricks.isNull(u_node)) {
				u_clock = PairHardCodeWordTricks.getClock(u_clocks);
				this.detachFromNeighbors(uprime_tid, u_node);
			}
			this.clocks[uprime_tid] = uprime_clocks;
			short y = NeibhorsHardCodeWordTricks.getParent(tc.tree[uprime_tid]);
			short head_child = NeibhorsHardCodeWordTricks.getHeadChild(this.tree[y]);
			if(!NeibhorsHardCodeWordTricks.isHeadChildNull(this.tree[y])) {
				this.tree[head_child] = NeibhorsHardCodeWordTricks.setPrevious(uprime_tid, this.tree[head_child]);
			}
			u_node = NeibhorsHardCodeWordTricks.setNextAndParent(head_child, y, u_node);
			this.tree[uprime_tid] = u_node;
			this.tree[y] = NeibhorsHardCodeWordTricks.setHeadChild(uprime_tid, this.tree[y]);
			
			vprime_tid = NeibhorsHardCodeWordTricks.getHeadChild(tc.tree[uprime_tid]);
			while(!NeibhorsHardCodeWordTricks.isTNull(vprime_tid)) {
				long vprime_clocks = tc.clocks[vprime_tid];
				int v_clock = this.getLocalClock(vprime_tid);
				if(v_clock < PairHardCodeWordTricks.getClock(vprime_clocks)) {
					this.S[++this.top] = vprime_tid;
				}
				else {
					if(vprime_tid == this.rootTid) {
						this.S[++this.top] = vprime_tid;
					}
					if(PairHardCodeWordTricks.getPclock(vprime_clocks) <= u_clock) {
						break;
					}
				}
				vprime_tid = NeibhorsHardCodeWordTricks.getNext(tc.tree[vprime_tid]);
			} 
		}

		this.rootTid = zprime_tid;
		

		return true;
	}

	@Override
	public String toString() {
		return "Dim: " + this.dim + "\nRoot Tid: " + this.rootTid + "\nTree: " +  this.tree2str() + "\nClocks: " + this.clocks2str();
	}

	public String toStringIgnoreParentOfRoot() {
		return "Dim: " + this.dim + "\nRoot Tid: " + this.rootTid + "\nTree: " +  this.tree2strIgnoreParentOfRoot() + "\nClocks: " + this.clocks2strIgnoreParentOfRoot();
	}

	@Override
	public boolean equals(Object other) {
		return this.toStringIgnoreParentOfRoot().equals(((TreeClock) other).toStringIgnoreParentOfRoot());
	}

	public String tree2str() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (long i : this.tree)
		{
			//sb.append(Long.toHexString(i));
			sb.append(i);
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public String tree2strIgnoreParentOfRoot() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int j=0; j < this.dim; j++)
		{		
			long i = this.tree[j];
			if(j == this.rootTid) {
				i = NeibhorsHardCodeWordTricks.setNextNull(i);
				i = NeibhorsHardCodeWordTricks.setPreviousNull(i);
				i = NeibhorsHardCodeWordTricks.setParentNull(i);
			}

			sb.append(Long.toHexString(i));
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public String clocks2str() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (long i : this.clocks)
		{
			sb.append(Long.toHexString(i));
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public String clocks2strIgnoreParentOfRoot() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int j=0; j < this.dim; j++)
		{
			long i = this.tree[j];
			if(j == this.rootTid) {
				i = PairHardCodeWordTricks.setPclock(0, i);
			}
			sb.append(Long.toHexString(i));
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public String timesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(short i=0; i<this.dim; i++) {
			sb.append(this.getLocalClock(i));
			if(i<this.dim - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}