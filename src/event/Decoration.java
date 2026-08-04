package event;

import java.io.Serializable;

public abstract class Decoration implements Serializable {
	protected int id;
	protected String name;
	
	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String toString() {
		return getName();
		//return "[Variable-" + Integer.toString(this.id) + "-" + this.name + "]";
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}
}
