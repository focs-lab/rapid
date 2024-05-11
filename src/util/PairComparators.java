package util;

import java.util.Comparator;

public class PairComparators{
	public static class FirstComparator<A extends Comparable<A>, B> implements Comparator<Pair<A, B>> {
		@Override
		public int compare(Pair<A, B> o1, Pair<A, B> o2) {
			return o1.first.compareTo(o2.first);
		}
	}
	
	public static class SecondComparator<A, B extends Comparable<B>> implements Comparator<Pair<A, B>> {
		@Override
		public int compare(Pair<A, B> o1, Pair<A, B> o2) {
			return o1.second.compareTo(o2.second);
		}
	}
}
