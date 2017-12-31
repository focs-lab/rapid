package parse.rr;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import event.EventType;
import parse.util.CannotParseException;
import parse.util.EventInfo;

public class Parse {
	// ACQUIRE, RELEASE, READ, WRITE, FORK, JOIN, BEGIN, END;
	public static String matchStr[] = { "Acquire", "Release", "[A]?Rd", "[A]?Wr", "Start", "Join", "Enter", "Exit", "Dummy" };
	public static String prefixPattern = "^@[\\s]+(";
	public static String suffixPattern = ")[(]([^,\\s]+)[,]([^,\\s]+)[)]([\\s]+)?([^,\\s]+)?([\\s]+)?([^,\\s]+)?";
	public static String stringEventPattern = prefixPattern + String.join("|", matchStr) + suffixPattern;
	public static Pattern eventPattern = Pattern.compile(stringEventPattern);
	public HashMap<String, EventType> mapMatchType;

	public Parse() {
		mapMatchType = new HashMap<String, EventType>();
		for (EventType type : EventType.values()) {
			String tp_str = matchStr[type.ordinal()];
			if(tp_str.equals("[A]?Rd") || tp_str.equals("[A]?Wr")){
				tp_str = tp_str.substring(4);
			}
			mapMatchType.put(tp_str, type);
		}
	}

	public static void example() {
		String line = "@    ARd(2,null.test/Deadlock.value_I)  Final  Deadlock.java:50:7";
//		String line = "@    Acquire(2,@03)";
//		String line = "@   Exit(1,test/Deadlock.doSomething()V)";
		Parse parse = new Parse();
		EventInfo eInfo = new EventInfo();
		try{
			parse.getInfo(eInfo, line);
		}
		catch(CannotParseException e){
			System.out.println("Could not parse  !");
		}
		System.out.println(eInfo);
	}

	public void getInfo(EventInfo eInfo, String line) throws CannotParseException {
		Matcher matcher = eventPattern.matcher(line);
		if (matcher.find()) {
			
			String tp_str = matcher.group(1);
			if(tp_str.equals("ARd") || tp_str.equals("AWr")){
				tp_str = tp_str.substring(1);
			}
			EventType tp = mapMatchType.get(tp_str);
			String thId = matcher.group(2);
			String aux = matcher.group(3);
			String locId = "";
			if(tp.isAccessType()){
				locId = matcher.group(7);
				if(locId == null){
					throw new CannotParseException(line);
				}
			}
			eInfo.updateEventInfo(tp, thId, aux, locId);
		} else {
			throw new CannotParseException(line);
		}
	}

	public static void main(String args[]){
		example();
	}
}
