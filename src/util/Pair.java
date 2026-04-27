package util;

import java.util.Objects;
import java.io.Serializable;

public class Pair<T, U> implements Serializable{
	public T first;
	public U second;
	public Pair(T f, U s){
		this.first = f;
		this.second = s;
	}
	public boolean equals(Pair<T, U> other){
       return this.first.equals(other.first) && this.second.equals(other.second);
    }

    public int hashCode(){
      return Objects.hash(first, second);
    }
    
    public String toString(){
    	return "<" + first.toString() + ", " + second.toString() + ">"; 
    }
}
