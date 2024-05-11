package parse.csv;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import event.EventType;
import parse.util.CannotParseException;
import parse.util.EventInfo;

public class Parse {
	// ACQUIRE, RELEASE, READ, WRITE, FORK, JOIN;
		public static String matchStr[] = { "acq", "rel", "r", "w", "start", "join", "enter", "exit", "dummy" };
		
		public static String prefixPattern = "^(";
		public static String midFixPattern = String.join("|", matchStr);
		public static String suffixPattern = ")[(]([^\\s]+)[)]$";
		public static String stringEventPattern = prefixPattern + midFixPattern + suffixPattern;
		public static Pattern primitiveEventPattern = Pattern.compile(stringEventPattern);
		public HashMap<String, EventType> mapMatchType;
		
		public static String cvsSplitBy = ",";
		public static String stringGenericEventPattern = prefixPattern + midFixPattern + "|sync" + suffixPattern;
		public static Pattern genericEventPattern = Pattern.compile(stringGenericEventPattern);

		public Parse() {
			mapMatchType = new HashMap<String, EventType>();
			for (EventType type : EventType.values()) {
				mapMatchType.put(matchStr[type.ordinal()], type);
			}
		}

		public static void example() {
			String line = ",,,,sync(z),,";
			Parse parse = new Parse();
			try{
				System.out.println(parse.getInfoList(line));
			}
			catch(CannotParseException e){
				System.out.println("Could not parse  !");
			}
		}
	
		public EventInfo getInfo(int tIndex, Matcher matcher) {
			String strType = matcher.group(1);
			EventType tp = mapMatchType.get(strType);
			String th = "T" + Integer.toString(tIndex);
			String aux = matcher.group(2);
			EventInfo str = new EventInfo(tp, th, aux, "");
			return str;
		}
		
		public ArrayList<EventInfo> getInfoList(String line) throws CannotParseException {
			String[] tArray = line.split(cvsSplitBy, -1);
			int len = Array.getLength(tArray);
			int tIndex = -1;
			String restInfo = null;
			ArrayList<EventInfo> infoList = null;
			for(int i = 0; i < len; i ++){
				if(!(tArray[i].equals(""))){
					tIndex = i;
					restInfo = tArray[i];
					break;
				}
			}
			if(tIndex < 0){
				throw new CannotParseException(line);
			}
			else{
				Matcher matcher = genericEventPattern.matcher(restInfo);
				if (matcher.find()) {
					infoList = new ArrayList<EventInfo>();
					Matcher primitiveMatcher = primitiveEventPattern.matcher(restInfo);
					if(primitiveMatcher.find()){
						//Primitive pattern
						infoList.add(getInfo(tIndex, primitiveMatcher));
					}
					else{
						//Sync type
						String var = matcher.group(2);
						String varLock = "VARLOCK-" + var + "-KCOLRAV"; 
						Matcher subMatcher = null;
						
						String e1 = "acq(" + varLock + ")";
						subMatcher = primitiveEventPattern.matcher(e1);
						subMatcher.find();
						infoList.add(getInfo(tIndex, subMatcher));
						
						String e2 = "r(" + var + ")";
						subMatcher = primitiveEventPattern.matcher(e2);
						subMatcher.find();
						infoList.add(getInfo(tIndex, subMatcher));
						
						String e3 = "w(" + var + ")";
						subMatcher = primitiveEventPattern.matcher(e3);
						subMatcher.find();
						infoList.add(getInfo(tIndex, subMatcher));
						
						String e4 = "rel(" + varLock + ")";
						subMatcher = primitiveEventPattern.matcher(e4);
						subMatcher.find();
						infoList.add(getInfo(tIndex, subMatcher));
					}
				} else {
					throw new CannotParseException(line);
				}
			}
			return infoList;
		}
}
