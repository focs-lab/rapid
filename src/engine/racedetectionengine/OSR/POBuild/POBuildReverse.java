package engine.racedetectionengine.OSR.POBuild;

import org.apache.commons.io.input.ReversedLinesFileReader;
import util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class POBuildReverse {

    private String traceDir;
    public Map<String, Integer> threadNameToId; //thName -> thId
    public Map<Integer, Integer> threadNameToNumEvents; // thread name -> number of events in that thread
    public Map<Integer, Integer> threadNameToCnt;
    public Map<String, Set<Integer>> varNameToThreads; // variable name -> set(id of threads accessed this var)
    public Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode; // fromThId -> toThId -> succ array (inThId of fromNode)
    public Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode; // fromThId -> toThId -> succ array (inThId of toNode)
    public Set<String> threads;

    public Map<Integer, int[]> joinMap; // T1|join(T2) T2.thId -> [T1.thId, inThreadId of join event]
    public Map<Integer, Map<String, Integer>> recentWrite; // threadId -> varName -> recentWrite inThreadId
    public Map<Integer, Map<String, Pair<Long, Integer>>> recentRead; // threadId -> varName -> recentRead auxId, inThreadId
    public Map<Integer, Map<String, Integer>> recentAcq; // threadId -> lockName -> recentRel inThreadId
    public Map<Integer, Map<Integer, Integer>> upperBounds; // fromThId -> toThId -> lower bound of the value in array

    public Map<String, Long> globalLastWrite; // varId -> global last write auxId
    public long totalNumEvents;
    public long totalEventsSeen;
    public int numThreads;
    public ArrayList<Long>[] inThreadIdToAuxId;

    public POBuildReverse(){}

    public POBuildReverse(String traceDir) {
        this.traceDir = traceDir;
        try {
            this.totalNumEvents = Files.lines(Paths.get(this.traceDir)).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.totalEventsSeen = 0L;

        this.varNameToThreads = new HashMap<>();
        this.recentAcq = new HashMap<>();
        this.recentRead = new HashMap<>();
        this.recentWrite = new HashMap<>();
        this.joinMap = new HashMap<>();
        this.threads = new HashSet<>();

        this.threadNameToNumEvents = new HashMap<>();
        this.threadNameToCnt = new HashMap<>();
        this.succFromNode = new HashMap<>();
        this.succToNode = new HashMap<>();
        this.upperBounds = new HashMap<>();
        this.threadNameToId = new HashMap<>();
        this.globalLastWrite = new HashMap<>();

        this.readMetaInfo();
    }

    public void readTraceAndUpdateDS(){
        ReversedLinesFileReader rf = null;
        try {
            File f = new File(this.traceDir);
            rf = new ReversedLinesFileReader(f);
            String line;
            line = rf.readLine();

            while(line != null){
                parseLine(line);
                line = rf.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void readMetaInfo(){
        String metaDir = "";

        if (this.traceDir.endsWith("std")) {
            metaDir = this.traceDir.substring(0, this.traceDir.length() - 3) + "ssp";
        } else {
            System.out.println("Please check input, the meta info file is not found !");
            System.exit(1);
        }

        try {
            BufferedReader in = new BufferedReader(new FileReader(metaDir));
            String line;

            // read total num of events
            line = in.readLine();
            this.totalNumEvents = Integer.parseInt(line.split("=")[1].strip());

            // read number of threads
            line = in.readLine();
            this.numThreads = Integer.parseInt(line.split("=")[1].strip());

            for(int i=0;i<this.numThreads;i++){
                line = in.readLine();
                String[] split = line.split("=");
                String threadName = split[0].strip();
                String[] threadInfo = split[1].strip().split(",");
                int threadId = Integer.parseInt(threadInfo[0]);
                int numEvents = Integer.parseInt(threadInfo[1]);

                this.threadNameToId.put(threadName, threadId);
                this.threadNameToCnt.put(threadId, 0);
                this.threadNameToNumEvents.put(threadId, numEvents);
                this.threads.add(threadName);

                this.recentRead.put(i, new HashMap<>());
                this.recentWrite.put(i, new HashMap<>());
                this.recentAcq.put(i, new HashMap<>());
            }

            // init succ
            for(int i=0;i<this.numThreads;i++){
                this.upperBounds.put(i, new HashMap<>());
                this.succFromNode.put(i, new HashMap<>());
                this.succToNode.put(i, new HashMap<>());

                for(int j=0;j<this.numThreads;j++){
                    if(i != j){
                        this.upperBounds.get(i).put(j, -1);
                        this.succFromNode.get(i).put(j, new ArrayList<>());
                        this.succToNode.get(i).put(j, new ArrayList<>());
                    }
                }
            }

            // init inThreadIdToAuxId
            this.inThreadIdToAuxId = (ArrayList<Long>[]) Array.newInstance(ArrayList.class, this.numThreads);
            for(int i=0;i<numThreads;i++){
                this.inThreadIdToAuxId[i] = new ArrayList<>();
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(this.totalNumEvents);
//        System.out.println(this.numThreads);
//        System.out.println(this.threadNameToNumEvents);
    }

    public void parseLine(String line){
        String[] info = line.split("\\|");
        String threadName = info[0];
        String op = info[1];
        String[] temp = op.split("\\(");
        String opCode = temp[0];
        String target = temp[1].substring(0, temp[1].length() - 1);

        if(opCode.equals("w")){
            handleWrite(threadName, target);
        } else if(opCode.equals("r")){
            handleRead(threadName, target);
        } else if(opCode.equals("acq")){
            handleAcq(threadName, target);
        } else if(opCode.equals("rel")){
            handleRel(threadName, target);
        } else if(opCode.equals("fork")){
            handleFork(threadName, target);
        } else if(opCode.equals("join")){
            handleJoin(threadName, target);
        } else{
            System.out.println("Invalid line : " + line);
            System.out.println("op : " + Arrays.toString(temp));
            System.exit(1);
        }
        totalEventsSeen++;
    }

    public void onNewVariableFound(String varName){
        if(!varNameToThreads.containsKey(varName)){
            varNameToThreads.put(varName, new HashSet<>());
        }
    }

    public void checkJoin(String threadName, int fromInThId){
        int fromThId = this.threadNameToId.get(threadName);

        if(joinMap.containsKey(fromThId)){

            int[] info = joinMap.get(fromThId);
            int toThId = info[0];
            int toEventInThreadId = info[1];

            // update succ
            this.addEdge(fromThId, toThId, fromInThId, toEventInThreadId);
            joinMap.remove(fromThId);

            // update lower bounds
            int currBound = this.upperBounds.get(fromThId).get(toThId);
            if(currBound == -1 || currBound > toEventInThreadId){
                this.upperBounds.get(fromThId).put(toThId, toEventInThreadId);
            }
        }

    }

    public void handleWrite(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);

        this.onNewVariableFound(target);

        this.checkJoin(threadName, inThreadId);
        Set<Integer> threads = varNameToThreads.get(target);
        for(Integer toThreadId : threads){

            if(threadId != toThreadId){
                boolean shouldSave = false;
                boolean shouldUpdateUpperBound = false;
                int toNodeInThId = -1;
                int readId = -1;

                if(recentWrite.get(toThreadId).containsKey(target)){
                    toNodeInThId = recentWrite.get(toThreadId).get(target);
                    shouldSave = true;
                }

                if(recentRead.get(toThreadId).containsKey(target)){
                    Pair<Long, Integer> readInfo = recentRead.get(toThreadId).get(target);
                    readId = readInfo.second;
                    long readAuxId = readInfo.first;
                    if(toNodeInThId == -1 || toNodeInThId > readId){
                        toNodeInThId = readId;
                    }
                    shouldSave = true;

                    if(globalLastWrite.containsKey(target)){
                        long lastWriteId = globalLastWrite.get(target);
                        if(lastWriteId > readAuxId) {
                            // this is indeed a read-from edge
                            shouldUpdateUpperBound = true;
                        }
                    } else {
                        // no other writes exist
                        shouldUpdateUpperBound = true;
                    }
                }

                if(shouldSave) {
                    this.addEdge(threadId, toThreadId, inThreadId, toNodeInThId);

                    if(shouldUpdateUpperBound){
                        // update upperBounds, this has to happen after edge insertion
                        int curBound = this.upperBounds.get(threadId).get(toThreadId);
                        if(curBound == -1 || curBound > readId){
                            this.upperBounds.get(threadId).put(toThreadId, readId);
                        }
                    }
                }
            }
        }

        // update recentWrite
        recentWrite.get(threadId).put(target, inThreadId);
        threadNameToCnt.put(threadId, curCnt + 1);
        globalLastWrite.put(target, auxId);
        varNameToThreads.get(target).add(threadId);
    }

    public void handleRead(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);
        this.onNewVariableFound(target);

        this.checkJoin(threadName, inThreadId);
        Set<Integer> threads = varNameToThreads.get(target);
        for(Integer toThId : threads){
            if(toThId != threadId){
                if(recentWrite.get(toThId).containsKey(target)){
                    int writeId = recentWrite.get(toThId).get(target);
                    this.addEdge(threadId, toThId, inThreadId, writeId);
                }
            }
        }

        // update recentRead
        recentRead.get(threadId).put(target, new Pair<>(auxId, inThreadId));
        threadNameToCnt.put(threadId, curCnt + 1);
        varNameToThreads.get(target).add(threadId);
    }

    public void handleFork(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        int toThreadId = this.threadNameToId.get(target);
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);
        this.checkJoin(threadName, inThreadId);

        this.addEdge(threadId, toThreadId, inThreadId, 1);

        // update upperBounds
        this.upperBounds.get(threadId).put(toThreadId, 1); // no need to compare, because 1 is the smallest inThreadId

        threadNameToCnt.put(threadId, curCnt + 1);
    }

    public void handleJoin(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        int targetThId = this.threadNameToId.get(target);
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);
        this.checkJoin(threadName, inThreadId);

        this.joinMap.put(targetThId, new int[]{threadId, inThreadId});

        threadNameToCnt.put(threadId, curCnt + 1);
    }

    public void handleAcq(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);
        this.checkJoin(threadName, inThreadId);

        // update recentRel
        recentAcq.get(threadId).put(target, inThreadId);
        threadNameToCnt.put(threadId, curCnt + 1);
    }

    public void handleRel(String threadName, String target) {
        int threadId = this.threadNameToId.get(threadName);
        int curCnt = threadNameToCnt.get(threadId);
        int totalCnt = threadNameToNumEvents.get(threadId);
        int inThreadId = totalCnt - curCnt;
        long auxId = totalNumEvents - totalEventsSeen;

        this.inThreadIdToAuxId[threadId].add(auxId);
        this.onNewVariableFound(target);

        this.checkJoin(threadName, inThreadId);

        Set<Integer> threads = varNameToThreads.get(target);

        for(Integer toThId : threads){
            if(toThId != threadId){
                if(recentAcq.get(toThId).containsKey(target)){
                    int acqId = recentAcq.get(toThId).get(target);
                    this.addEdge(threadId, toThId, inThreadId, acqId);
                }
            }
        }

        threadNameToCnt.put(threadId, curCnt + 1);
        varNameToThreads.get(target).add(threadId);
    }

    public void addEdge(int fromThId, int toThId, int fromInThId, int toInThId){
        if(fromThId == toThId) return;

        int upperBound = this.upperBounds.get(fromThId).get(toThId);

        if(upperBound == -1 || upperBound > toInThId){
            this.succFromNode.get(fromThId).get(toThId).add(fromInThId);
            this.succToNode.get(fromThId).get(toThId).add(toInThId);
        }
    }

    public void printArray(){
        long totalEdges = 0;
        long numRealEdges = 0;
        for(int i=0;i<this.numThreads;i++){
            totalEdges += (long) this.threadNameToNumEvents.get(i) * this.numThreads;

            for(int j=0;j<this.numThreads;j++){
                if(i == j) continue;

                ArrayList<Integer> fromNodes = this.succFromNode.get(i).get(j);
                ArrayList<Integer> toNodes = this.succToNode.get(i).get(j);

                numRealEdges += toNodes.size();

//                System.out.println(i + " -> " + j + " : " + toNodes.size());

                StringBuilder arrStr = new StringBuilder();
                for(int k=0;k<toNodes.size();k++){
                    arrStr.append(" (").append(fromNodes.get(k)).append(", ").append(toNodes.get(k)).append(") ");
                }

                System.out.println(i + " -> " + j + " : " + arrStr);
            }
        }
        double sparsity = 1.0 * numRealEdges / totalEdges;
        System.out.println("sparsity is : " + String.format("%.2f", sparsity));
    }


}
