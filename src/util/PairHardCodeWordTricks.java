package util;



public final class PairHardCodeWordTricks{
	
//The arrangement is PCLOCK - CLOCK
	public static final int CLOCK_BITS = 32;
	public static final int PCLOCK_BITS = 32;
	public static final long ALL_ONES_LONG = -1;
	public static final long PCLOCK_MASK =  (ALL_ONES_LONG >>> PCLOCK_BITS) <<  PCLOCK_BITS;
	public static final long CLOCK_MASK = (ALL_ONES_LONG << PCLOCK_BITS ) >>> PCLOCK_BITS;
	
	

	public static int getClock(long data) {
		return (int)( data & CLOCK_MASK  );
	}

	public static int getPclock(long data) {
		return (int)( data >> (CLOCK_BITS)  );
	}


	public static long copyClock(long from, long to) {
		to &= ~CLOCK_MASK;
		to |= from & CLOCK_MASK;
		return to;
	}

	public static long copyPclock(long from, long to) {
		to &= ~PCLOCK_MASK;
		to |= from & PCLOCK_MASK;
		return to;
	}


	public static long copyClockToPclock(long from, long to) {

		to &= ~PCLOCK_MASK;
		to |= (from & CLOCK_MASK) << CLOCK_BITS;

		return to;
	}

	

	public static long setPclock(int pclock, long to) {
		to &= ~PCLOCK_MASK;
		to |= ( pclock & CLOCK_MASK ) << CLOCK_BITS;
		return to;
	}

	public static long setClock(int clock, long to) {
		to &= ~CLOCK_MASK;
		to |= ( clock & CLOCK_MASK);
		return to;
	}


	public static long incrementClockBy(int inc, long to) {
		to += inc;
		return to;
	}

	public static boolean clockIsLessThan(long first, long second) {
		return (first & CLOCK_MASK) < (second & CLOCK_MASK);
	}

	public static boolean clockIsLessThanOrEqual(long first, long second) {
		return (first & CLOCK_MASK) <= (second & CLOCK_MASK);
	}
	
	
	
	

	public static String toString(int data) {		
	
		String str = "";
		str += "<";
		str += getClock(data);
		str += ", ";
		str += getPclock(data);
		str += ">";
		return str;
	}
}
