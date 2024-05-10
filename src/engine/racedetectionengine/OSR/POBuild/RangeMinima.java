package engine.racedetectionengine.OSR.POBuild;

import java.util.ArrayList;

public class RangeMinima {

    public NumArrayMin numArray;

    public RangeMinima(ArrayList<int[]> nums) {
        this.numArray = new NumArrayMin(nums);
    }

    public RangeMinima(RangeMinima other) {
        this.numArray = new NumArrayMin(other.numArray);
    }

    public int getMinWithRange(int leftInThId, int rightInThId){
        return this.numArray.sumRange(leftInThId, rightInThId);
    }

}
