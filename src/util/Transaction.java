package util;

import java.util.Objects;

import event.Thread;


public class Transaction {
	public int id;
	public Thread thread;
	public Transaction(){
		this.id = -1;
	}
	public Transaction(Thread t, int id){
		if(id == -1){
			throw new IllegalArgumentException("Trnsaction: Cannot use custom constructor for id=-1"); 
		}
		this.thread = t;
		this.id = id;
	}
	public boolean equals(Transaction other){
	       return this.id == other.id && this.thread.equals(other.thread);
	    }

    public int hashCode(){
      return Objects.hash(this.id, this.thread);
    }
    
    public String toString() {
    	if(thread != null) return "<" + thread.toString() + ":" + Integer.toString(id) + ">";
    	else return "<null:" + Integer.toString(id) + ">";
    }
}
