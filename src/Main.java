public class Main {
    public static void main(String[] args){
        if(args.length == 2){
            String algo = args[0];
            String trace_dir = args[1];

            switch (algo) {
                case "SyncP" -> SyncPreserving.analysis(trace_dir);
                case "SHB" -> SHB.analysis(trace_dir);
                case "OSR" -> OSR.analysis(trace_dir);
                case "WCP" -> WCP.analysis(trace_dir);
                case "WCP-Sound" -> WCPSound.analysis(trace_dir);
                default -> {
                    System.out.println(algo + " is not recognized. ");
                    System.out.println("Supported algo = [SyncP, SHB, OSR, WCP-Sound, WCP]");
                }
            }
        } else {
            System.out.println("Usage: java -jar [path-to-jar-file] [algo-name] [trace-loc]");
        }
    }
}
