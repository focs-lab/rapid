package util;

import java.util.Comparator;

public class TripletComparators{
	public static class FirstComparator<A extends Comparable<A>, B, C> implements Comparator<Triplet<A, B, C>> {
		@Override
		public int compare(Triplet<A, B, C> o1, Triplet<A, B, C> o2) {
			return o1.first.compareTo(o2.first);
		}
	}
	
	public static class SecondComparator<A, B extends Comparable<B>, C> implements Comparator<Triplet<A, B, C>> {
		@Override
		public int compare(Triplet<A, B, C> o1, Triplet<A, B, C> o2) {
			return o1.second.compareTo(o2.second);
		}
	}
	
	public static class ThirdComparator<A, B, C extends Comparable<C>> implements Comparator<Triplet<A, B, C>> {
		@Override
		public int compare(Triplet<A, B, C> o1, Triplet<A, B, C> o2) {
			return o1.third.compareTo(o2.third);
		}
	}
}
