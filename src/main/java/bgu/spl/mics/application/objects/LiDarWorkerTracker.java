package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    private int id;
    private int frequency;
    private STATUS status;
    private List<TrackedObject> lastTrackedObjects;
    protected final LiDarDataBase dataBase;
    protected final ErrorOutput errorOutput;


    public LiDarWorkerTracker(int id, int frequency, String filePath){
        this.id = id;
        this.frequency = frequency;
        status = STATUS.UP;
        lastTrackedObjects = new ArrayList<>();
        dataBase = LiDarDataBase.getInstance(filePath);
        errorOutput = ErrorOutput.getInstance();
    }

    public int getId(){
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public List<TrackedObject> getLastTrackedObjects() {
        return lastTrackedObjects;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    /**
     * gets data from camera via DetectedObjectEvent and looks for the same objects coordinates at the specific time
     * adds the relevant tracked object to lastTrackedObjects list
     */
    public  void checkData(StampedDetectedObjects stampedDetectedObjects){
        int time = stampedDetectedObjects.getTime();
        List<DetectedObject> objects = stampedDetectedObjects.getDetectedObjects();
        for (StampedCloudPoints stampedCloudPoints : dataBase.getStampedCloudPoints()){
            if (time == stampedCloudPoints.getTime()){
                for (DetectedObject obj : objects){
                    if (obj.getId().equals(stampedCloudPoints.getId())){
                        TrackedObject newTrackedObj = new TrackedObject(obj.getId(), time, obj.getDescription());
                        for (List<Double> coordinates : stampedCloudPoints.getCloudPoints()){
                            CloudPoint newCloudPoint = new CloudPoint(coordinates.get(0), coordinates.get(1));
                            newTrackedObj.addCloudPoint(newCloudPoint);
                        }
                        lastTrackedObjects.add(newTrackedObj);
                    }
                }
            }
        }
    }

    public List<TrackedObject> canSendTrackedObjects (int currTick){
        List<TrackedObject> trackedObjectList = new ArrayList<>();
        //search trackedObjects list for relevant objects to send
        for (TrackedObject object : lastTrackedObjects){
            //currTick is at least Detection time + lidar_frequency
            if (currTick >= object.getTime() + frequency){
                if (object.getId().equals("ERROR")){
                    status = STATUS.ERROR;
                    errorOutput.setError(object.getDescription());
                    errorOutput.setFaultySensor("LiDarWorkerTracker" + id);
                    StatisticalFolder.getInstance().setSystemRuntime(currTick);
                    return null;
                }
                trackedObjectList.add(object);
                dataBase.incTracked();
            }
        }
        if (!trackedObjectList.isEmpty()) {
            lastTrackedObjects.removeAll(trackedObjectList);
        }
        return trackedObjectList;
    }

    public boolean isFinished() {
        return dataBase.isFinished();
    }
}

