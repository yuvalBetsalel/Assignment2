package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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


    public LiDarWorkerTracker(int id, int frequency, String filePath){
        this.id = id;
        this.frequency = frequency;
        status = STATUS.UP;
        lastTrackedObjects = new ArrayList<>();
        dataBase = LiDarDataBase.getInstance(filePath);
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

    public LiDarDataBase getDataBase() {
        return dataBase;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

//    public void setDataFile(String filePath){
//        dataBase.setFilePath(filePath);
//    }
    /**
     * gets data from camera via DetectedObjectEvent and looks for the same objects coordinates at the specific time
     * adds the relevant tracked object to lastTrackedObjects list
     *
     */
    public List<TrackedObject> checkData(StampedDetectedObjects stampedDetectedObjects){
        int time = stampedDetectedObjects.getTime();
        List<DetectedObject> objects = stampedDetectedObjects.getDetectedObjects();
        //search for obj with same time as time
        for (StampedCloudPoints stampedCloudPoints : dataBase.getStampedCloudPoints()){
            if (time == stampedCloudPoints.getTime()){
                //search for obj with same id
                for (DetectedObject obj : objects){
                    if (obj.equals(stampedCloudPoints.getId())){
                        //found obj with same time and same id
                        TrackedObject newTrackedObj = new TrackedObject(obj.getId(), time, obj.getDescription());
                        //add all coordinates to tracked obj
                        for (List<Double> coordinates : stampedCloudPoints.getCloudPoints()){
                            CloudPoint newCloudPoint = new CloudPoint(coordinates.get(0), coordinates.get(1));
                            newTrackedObj.addCloudPoint(newCloudPoint);
                        }
                        //add tracked obj to lastTrackedObjects list
                        lastTrackedObjects.add(newTrackedObj);
                    }
                }
            }
        }
        return lastTrackedObjects;
    }

//    public void addTrackedObject (TrackedObject object) {
//        lastTrackedObjects.add(object);
//    }
}
