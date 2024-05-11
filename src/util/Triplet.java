package util;

import java.util.Objects;

public class Triplet<A, B, C> {
	public A first;
	public B second;
	public C third;
 	public Triplet(A a, B b, C c){
		this.first = a;
		this.second = b;
		this.third = c;
	}
	
 	@Override
 	public boolean equals(Object o){
       if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		Triplet<?, ?, ?> triplet = (Triplet<?, ?,?>) o;

		// call equals() method of the underlying objects
		if (!first.equals(triplet.first))
			return false;
		if (!second.equals(triplet.second))
			return false;
		return third.equals(triplet.third);
    }

    public int hashCode(){
      return Objects.hash(first, second, third);
    }
    
    public String toString(){
    	String str = "<";
    	str += first;
    	str += ", ";
    	str += second;
    	str += ", ";
    	str += third;
    	str += ">";
    	return str;
    }
}
