# OSR Artifact
1. ### Environment
   - JDK-21 or above
   - python3

2. ### Run race detection 
   - `java -jar "rapid.jar" [algo name] [dir to trace.std]`
   - Available [algo name] values: `OSR SHB SyncP WCP WCP-Sound OSR-Witness`
   - `WCP` is only sound for the first race
   - `WCP-Sound` is sound for all races
   - `OSR-Witness` will print the witness for each race it finds.