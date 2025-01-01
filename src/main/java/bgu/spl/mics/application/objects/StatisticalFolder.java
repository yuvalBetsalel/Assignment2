package bgu.spl.mics.application.objects;

import bgu.spl.mics.MessageBusImpl;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private static class StatisticalFolderHolder {
        private static StatisticalFolder instance = new StatisticalFolder();
    }


    private int systemRuntime;
    private int numDetectedObjects;
    private int numTrackedObjects;
    private int numLandmarks;

    public StatisticalFolder(){
        systemRuntime = 0;
        numDetectedObjects= 0;
        numTrackedObjects = 0;
        numLandmarks = 0;
    }

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    public int getSystemRuntime() {
        return systemRuntime;
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects;
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects;
    }

    public int getNumLandmarks() {
        return numLandmarks;
    }

    public void setSystemRuntime(int systemRuntime) { // uses TimeService getCounter
        this.systemRuntime = systemRuntime;
    }

    public void addDetectedObjects(){
        numDetectedObjects++;
    }

    public void addTrackedObjects(){
        numTrackedObjects++;
    }

    public void addLandmarks(){
        numLandmarks++;
    }

}
