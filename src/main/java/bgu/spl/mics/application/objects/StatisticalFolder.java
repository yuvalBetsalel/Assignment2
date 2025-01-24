package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private static class StatisticalFolderHolder {
        private static StatisticalFolder instance = new StatisticalFolder();
    }
    private AtomicInteger systemRuntime;
    private AtomicInteger numDetectedObjects;
    private AtomicInteger numTrackedObjects;
    private AtomicInteger numLandmarks;
    private List<LandMark> landMarks;

    public StatisticalFolder(){
        systemRuntime = new AtomicInteger(0);
        numDetectedObjects= new AtomicInteger(0);
        numTrackedObjects = new AtomicInteger(0);
        numLandmarks = new AtomicInteger(0);
        landMarks = new CopyOnWriteArrayList<>();
    }

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    public AtomicInteger getSystemRuntime() {
        return systemRuntime;
    }

    public AtomicInteger getNumDetectedObjects() {
        return numDetectedObjects;
    }

    public AtomicInteger getNumTrackedObjects() {
        return numTrackedObjects;
    }

    public AtomicInteger getNumLandmarks() {
        return numLandmarks;
    }

    public List<LandMark> getLandMarks() {
        return landMarks;
    }

    public void setLandMarks(List<LandMark> newLandMarks) {
        this.landMarks = newLandMarks;
    }

    public void setSystemRuntime(int systemRuntime) { // uses TimeService getCounter
        this.systemRuntime.set(systemRuntime);
    }

    public void addDetectedObjects(int i){
        this.numDetectedObjects.addAndGet(i);
    }

    public void addTrackedObjects(int i){
        this.numTrackedObjects.addAndGet(i);
    }

    public void addLandmarks(){
        this.numLandmarks.incrementAndGet();
    }
}
