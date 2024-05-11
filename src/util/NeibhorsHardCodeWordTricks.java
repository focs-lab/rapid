package util;

// Using word tricks to store the neighbors (parent, first child, left sibling, right sibling) of a node in the tree clock

public class NeibhorsHardCodeWordTricks {
	//The arrangement is next-previous-parent-headchild
	public static final int TID_BITS = 16;
	public static final long ALL_ONES_LONG = -1;
	public static final short ALL_ONES_SHORT = -1;
	public static final long NULL = (long) 0;
	public static final short T_NULL = -1;
	
	public static final long NEXT_MASK = ((ALL_ONES_LONG >>> ( 3 * TID_BITS ) ) << ( 3 * TID_BITS) ) >>> ( 0 * TID_BITS);
	public static final long PREVIOUS_MASK = ((ALL_ONES_LONG >>> ( 2 * TID_BITS ) ) << ( 3 * TID_BITS) ) >>> ( 1 * TID_BITS);
	public static final long PARENT_MASK = ((ALL_ONES_LONG >>> ( 1 * TID_BITS ) ) << ( 3 * TID_BITS) ) >>> ( 2 * TID_BITS);
	public static final long HEADCHILD_MASK = ((ALL_ONES_LONG >>> ( 0 * TID_BITS ) ) << ( 3 * TID_BITS) ) >>> ( 3 * TID_BITS);
	public static final long NEXT_AND_PARENT_MASK = NEXT_MASK | PARENT_MASK;
	
	
	
	public static final short getNext(long data) {
		return (short) (( data >>> (3*TID_BITS) ) -1);
	}
	
	public static final short getPrevious(long data) {
		return (short) (( (data & PREVIOUS_MASK) >>> (2*TID_BITS) ) -1 );
	}
	
	public static final short getParent(long data) {
		return (short) (( (data & PARENT_MASK) >>> (1*TID_BITS) ) - 1);
	}
	
	public static final short getHeadChild(long data) {
		return (short) (( data & HEADCHILD_MASK  ) - 1);
	}
	
	public static final long setNext(short next, long to) {
		to &= ~NEXT_MASK;
		to |= ( (next+1) & HEADCHILD_MASK) << (3 * TID_BITS);
		return to;
	}
	
	public static final long setPrevious(short previous, long to) {
		to &= ~PREVIOUS_MASK;
		to |= ( (previous+1) & HEADCHILD_MASK ) << (2 * TID_BITS);
		return to;
	}
	
	public static final long setParent(short parent, long to) {
		to &= ~PARENT_MASK;
		to |= ( (parent+1) & HEADCHILD_MASK ) << (1 * TID_BITS);
		return to;
	}
	
	public static final long setNextAndParent(short next, short parent, long to) {
			to &= ~NEXT_AND_PARENT_MASK;
			to |= ( (parent+1) & HEADCHILD_MASK ) << (1 * TID_BITS);
			to |= ( (next+1) & HEADCHILD_MASK) << (3 * TID_BITS);
			return to;
	}
	
	public static final long setHeadChild(short headChild, long to) {	
		to &= ~HEADCHILD_MASK;
		to |= ( (headChild+1) & HEADCHILD_MASK );
		return to;
	}
	
	public static final long setNextNull(long data) {
		data &= ~NEXT_MASK;
		return data;
	}
	
	public static final long setPreviousNull(long data) {
		data &= ~PREVIOUS_MASK;
		return data;
	}
	
	public static final long setParentNull(long data) {
		data &= ~PARENT_MASK;
		return data;
	}
	
	public static final long setHeadChildNull(long data) {
		data &= ~HEADCHILD_MASK;
		return data;
	}
	
	public static final boolean isNull(long data) {
		return data == NULL;
	}
	
	public static final boolean isTNull(short t) {
		return t == T_NULL;
	}
	
	public static final boolean isNextNull(long data) {
		return ( data & NEXT_MASK ) == 0;
	}
	
	public static final boolean isPreviousNull(long data) {
		return ( data & PREVIOUS_MASK ) == 0;
	}
	
	public static final boolean isParentNull(long data) {
		return ( data & PARENT_MASK ) == 0;
	}
	
	public static final boolean isHeadChildNull(long data) {
		return ( data & HEADCHILD_MASK ) == 0;
	}
	
	public static final long copyNextToHeadChild(long from, long to) {
		to = (to & ~HEADCHILD_MASK) | ((from & NEXT_MASK) >>> (3 * TID_BITS));
		return to;
	}
	
	public static final long copyHeadChildToNext(long from, long to) {
		to = (to & ~NEXT_MASK) | ((from & HEADCHILD_MASK) << (3 * TID_BITS));
		return to;
	}
	
	public static final long copyHeadChildToHeadChild(long from, long to) {
		to = (to & ~HEADCHILD_MASK) | ((from & HEADCHILD_MASK));
		return to;
	}
	
	public static final long copyNextToNext(long from, long to) {
		to = (to & ~NEXT_MASK) | (from & NEXT_MASK);
		return to;
	}
	
	public static final long copyPreviousToPrevious(long from, long to) {
		to = (to & ~PREVIOUS_MASK) | (from & PREVIOUS_MASK);
		return to;
	}
	
	
	
	
	public static String toString(long data) {		

		//String str = String.format("0x%016x", data) + " - ";
		String str = "";
		str += "<";
		str += getNext(data)-1;
		str += ", ";
		str += getPrevious(data)-1;
		str += ", ";
		str += getParent(data)-1;
		str += ", ";
		str += getHeadChild(data)-1;
		str += ">";
		return str;
	}

}
