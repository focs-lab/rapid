package event;

public abstract class Decoration {
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

}
