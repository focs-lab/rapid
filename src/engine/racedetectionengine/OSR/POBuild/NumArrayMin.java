package engine.racedetectionengine.OSR.POBuild;

import java.util.ArrayList;

public class NumArrayMin {

	public ArrayList<int[]> nums;

	class SegmentTreeNode {
		public int start, end;
		public SegmentTreeNode left, right;
		public int min;

		public SegmentTreeNode(int start, int end) {
			this.start = start;
			this.end = end;
			this.left = null;
			this.right = null;
			this.min = Integer.MAX_VALUE;
		}
	}

	public SegmentTreeNode root = null;

	public NumArrayMin(ArrayList<int[]> nums) {
		this.nums = nums;
		this.root = buildTree(nums, 0, nums.size()-1);
	}

	public NumArrayMin(NumArrayMin other) {
 		this.root = other.root;
	}

	//Return the value at i
	public int get(int i) {
		return this.sumRange(root, i, i);
	}


	private SegmentTreeNode buildTree(ArrayList<int[]> nums, int start, int end) {
		if (start > end) {
			return null;
		} else {
			SegmentTreeNode ret = new SegmentTreeNode(start, end);
			if (start == end) {
				ret.min = nums.get(start)[1];
			} else {
				int mid = start  + (end - start) / 2;             
				ret.left = buildTree(nums, start, mid);
				ret.right = buildTree(nums, mid + 1, end);
				ret.min = ret.left.min <  ret.right.min ? ret.left.min : ret.right.min;
			}         
			return ret;
		}
	}

	void update(int i, int val) {
		update(root, i, val);
	}

	void update(SegmentTreeNode root, int pos, int val) {
		if (root.start == root.end) {
			root.min = val;
		} else {
			int mid = root.start + (root.end - root.start) / 2;
			if (pos <= mid) {
				update(root.left, pos, val);
			} else {
				update(root.right, pos, val);
			}
			root.min = root.left.min < root.right.min? root.left.min : root.right.min;
		}
	}

	public int argMin(int x) {
		if(this.root.min <= x) {
			return this.argMin(this.root, x)-1;
		}
		else {
			return -1;
		}
	}

	public int argMin(SegmentTreeNode root, int x) {
		if(root.left == root.right) {
			return 1;
		}
		if(root.right.min <= x) {
			return this.argMin(root.right, x) + root.left.end - root.left.start + 1;
		}
		else {
			return this.argMin(root.left, x);
		}
	}

	// find the largest index idx in nums, s.t. nums[idx] >= val
	public int searchIndexLeft(int val){
		if(this.nums.get(0)[0] < val) return -1;
		int size = this.nums.size();
		int left = 0, right = size - 1;
		int mid = -1;

		while(left < right){
			mid = (left + right) / 2;
			int midVal = nums.get(mid)[0];

			if(midVal == val) return mid;
			else if(midVal > val){
				if(mid + 1 < size){
					if(this.nums.get(mid + 1)[0] >= val){
						left = mid + 1;
					} else {
						return mid;
					}
				} else {
					return size - 1;
				}
			} else {
				right = mid - 1;
			}

//			System.out.println("left : " + left + ", " + right + ", " + mid);
		}

		return left;
	}

	// find the smallest index idx in nums, s.t. nums[idx] <= val
	public int searchIndexRight(int val){
		if(this.nums.get(this.nums.size()-1)[0] > val) return this.nums.size();

		int left = 0, right = this.nums.size() - 1;
		int mid = -1;

		while(left < right){
			mid = (left + right) / 2;
			int midVal = nums.get(mid)[0];

			if(midVal == val) return mid;
			else if(midVal > val){
				left = mid + 1;
			} else {
				if(mid - 1 >= 0){
					if(this.nums.get(mid - 1)[0] <= val){
						right = mid - 1;
					} else {
						return mid;
					}
				} else {
					return 0;
				}
			}

//			System.out.println("right : " + left + ", " + right + ", " + mid);
		}

		return left;
	}

	public int sumRange(int i, int j) {

		if(this.nums.size() == 0) return -1;

//		this.printArray();

		int right = searchIndexLeft(i);
		int left = searchIndexRight(j);

//		System.out.println("input : " + i + ", " + j);
//		System.out.println("query : " + left + ", " + right);

		if(left < 0 || right >= this.nums.size() || left > right) return -1;

		return sumRange(root, left, right);
	}

	public void printArray(){
		for(int[] temp : this.nums){
			System.out.print(" (" + temp[0] + ", " + temp[1] + ") ");
		}
		System.out.print("\n");
	}

	public int sumRange(SegmentTreeNode root, int start, int end) {

		if (root.end == end && root.start == start) {
			return root.min;
		} else {
			int mid = root.start + (root.end - root.start) / 2;
			if (end <= mid) {
				return sumRange(root.left, start, end);
			} else if (start >= mid+1) {
				return sumRange(root.right, start, end);
			}  else {
				int l = sumRange(root.left, start, mid);
				int r = sumRange(root.right, mid+1, end);


				return  l < r ? l : r ;
			}
		}
	}
}