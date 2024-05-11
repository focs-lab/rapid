package parse.util;

public class CannotParseException extends Exception {
	
	private static final long serialVersionUID = -2061251787621673954L;
	//private static final long serialVersionUID = 1L;
	private String line;

	public CannotParseException(String str) {
		this.line = str;
	}

	public String getLine() {
		return this.line;
	}
}