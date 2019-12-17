package parse.std;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import event.EventType;
import parse.util.CannotParseException;
import parse.util.EventInfo;

public class Parse {
	// ACQUIRE, RELEASE, READ, WRITE, FORK, JOIN, BEGIN, END, BRANCH;
		public static String matchStr[] = { "acq", "rel", "r", "w", "fork", "join", "begin", "end", "dummy" };
		
		public static String prefixPattern = "^(";
		public static String midFixPattern = String.join("|", matchStr);
		public static String suffixPattern = ")[(]([^\\s]+)[)]$";
		public static String stringEventPattern = prefixPattern + midFixPattern + suffixPattern;
		public static Pattern primitiveEventPattern = Pattern.compile(stringEventPattern);
		public HashMap<String, EventType> mapMatchType;
		
		public static String splitBy = "\\|";
		public static String stringGenericEventPattern = prefixPattern + midFixPattern + "|sync" + suffixPattern;
		public static Pattern genericEventPattern = Pattern.compile(stringGenericEventPattern);
		
		//public EventInfo eInfo;

		public Parse() {
			mapMatchType = new HashMap<String, EventType>();
			for (EventType type : EventType.values()) {
				mapMatchType.put(matchStr[type.ordinal()], type);
			}
		}

		public static void example() {
//			String line = "345|T20|sync(z)";
//			String line = "T20|join(T1)|345";
			String line = "T20|branch|345";
			Parse parse = new Parse();
			EventInfo eInfo = new EventInfo();
			try{
				parse.getInfo(eInfo, line);
			}
			catch(CannotParseException e){
				System.out.println("Could not parse  !");
			}
			System.out.println("Parsed successfully : " + eInfo);
		}
	
		public void getInfoOp(EventInfo eInfo, String th, String loc, Matcher matcher) {
			String strType = matcher.group(1);
			EventType tp = mapMatchType.get(strType);
			String aux = matcher.group(2);
			eInfo.updateEventInfo(tp, th, aux, loc);
		}
		
		public void getInfo(EventInfo eInfo, String line) throws CannotParseException {
			String[] eArray = line.split(splitBy, -1);
			if(eArray.length < 3 || eArray.length > 3){
				throw new CannotParseException(line);
			}
			else{
				String thId = eArray[0];
				String op = eArray[1];
				String locId = eArray[2];
				if(op.startsWith("begin")){
					eInfo.updateEventInfo(EventType.BEGIN, thId, null, locId);
				}
				else if(op.startsWith("end")){
					eInfo.updateEventInfo(EventType.END, thId, null, locId);
				}
				else{
					Matcher matcher = genericEventPattern.matcher(op);
					if (matcher.find()) {
						Matcher primitiveMatcher = primitiveEventPattern.matcher(op);
						if(primitiveMatcher.find()){
							getInfoOp(eInfo, thId, locId, primitiveMatcher);
						}
						else{
							throw new CannotParseException(line);
						}
					} else {
						throw new CannotParseException(line);
					}
				}
				
			}
		}
		
		public static void main(String args[]){
			example();
		}
}
