package util;

import java.util.Objects;

public class Quintet<A, B, C, D, E> {
	public A first;
	public B second;
	public C third;
	public D fourth;
	public E fifth;
	
 	public Quintet(A a, B b, C c, D d, E e){
		this.first = a;
		this.second = b;
		this.third = c;
		this.fourth = d;
		this.fifth = e;
	}
	
 	@Override
 	public boolean equals(Object o){
       if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		Quintet<?, ?, ?, ?, ?> quintet = (Quintet<?, ?, ?, ?, ?>) o;

		if (!first.equals(quintet.first))
			return false;
		if (!second.equals(quintet.second))
			return false;
		if (!third.equals(quintet.third))
			return false;
		if (!fourth.equals(quintet.fourth))
			return false;
		return fifth.equals(quintet.fifth);
    }

    public int hashCode(){
      return Objects.hash(first, second, third, fourth, fifth);
    }
    
    public String toString(){
    	String str = "<";
    	str += first;
    	str += ", ";
    	str += second;
    	str += ", ";
    	str += third;
    	str += ", ";
    	str += fourth;
    	str += ", ";
    	str += fifth;
    	str += ">";
    	return str;
    }
}
