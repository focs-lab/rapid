import sys, os

tracefiles = []

def stat(traceDir):
    metaInfoDir = traceDir.replace("std", "ssp")
    threads = {}
    trace = open(traceDir, "r")
    cnt = 0
    line = trace.readline()

    eventCnt = {'w': 0, 'r' : 0, 'fork' : 0, 'acq': 0, 'rel' : 0, 'join' : 0}

    varCnt = set()

    lockCnt = set()

    while line:
        cnt += 1
        thread = line.split("|")[0]
        op = line.split("|")[1]
        opcode = op.split("(")[0]
        target = op.split("(")[1].split(")")[0]

        eventCnt[opcode] += 1

        if thread not in threads:
            numThreads = len(threads)
            threads[thread] = [numThreads, 0]
        threads[thread][1] += 1

        if opcode == "r" or opcode == "w":
            if target not in varCnt:
                varCnt.add(target)

        elif opcode == 'acq' or opcode == 'rel':
            if target not in lockCnt:
                lockCnt.add(target)

        elif opcode == "fork":
            if target not in threads:
                numThreads = len(threads)
                threads[target] = [numThreads, 0]

        line = trace.readline()

    trace.close()


    # write to .ssp file
    metaInfoFile = open(metaInfoDir, "w")
    metaInfoFile.write("num of events = " + str(cnt) + "\n")
    metaInfoFile.write("num of threads = " + str(len(threads)) + "\n")
    for key in threads:
        metaInfoFile.write(key + " = " + str(threads[key][0]) + "," + str(threads[key][1]) + "\n")

    ls = getThreadsLen(threads)

    metaInfoFile.write("threads length = ")
    for i in range(len(ls)):
        if i != len(ls) -1:
            metaInfoFile.write(str(ls[i]) + ",")
        else:
            metaInfoFile.write(str(ls[i]) + "\n")
    print(ls)

    metaInfoFile.write("num locks = " + str(len(lockCnt)) + "\n")
    metaInfoFile.write("num vars = " + str(len(varCnt)) + "\n")



def getThreadsLen(threads):
    length = len(threads)
    ret = []

    for i in range(length):
        for key in threads:
            if(threads[key][0] == i):
                ret.append(threads[key][1])

    return ret


def run_recursive(path):
    if os.path.isdir(path):
        for sub in os.listdir(path):
            run_recursive(os.path.join(path, sub))
    elif path.endswith(".std"):
        tracefiles.append(path)


def main():
    traceDir = sys.argv[1]
    run_recursive(traceDir)

    for item in tracefiles:
        try:
            stat(item)
        except:
            print(item)


main()
