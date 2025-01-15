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
    //private List<StampedCloudPoints> waitingList;
    //private Map<Integer, List<TrackedObject>> waitingList;





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

    public LiDarDataBase getDataBase() {
        return dataBase;
    }

//    public Map<Integer, List<TrackedObject>> getWaitingList() {
//        return waitingList;
//    }
//
//    public void addToWaitingList(StampedCloudPoints stampedCloudPoints){
//        waitingList.add(stampedCloudPoints);
//    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public void deleteLastTracked(List<TrackedObject> trackedObjects){
        lastTrackedObjects.removeAll(trackedObjects);
    }
//    public void setDataFile(String filePath){
//        dataBase.setFilePath(filePath);
//    }
    /**
     * gets data from camera via DetectedObjectEvent and looks for the same objects coordinates at the specific time
     * adds the relevant tracked object to lastTrackedObjects list
     *
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
                        System.out.println(time + " objects: " + lastTrackedObjects);
                    }
                }
            }
        }
//        //add tracked obj to waiting list with key - time to send (detected time + freq)
//        if (!waitingList.containsKey(time + frequency)) {
//            waitingList.put(time + frequency, newTrackedObjects);
//        }
//        else{       //if there is already objects with same key (not sure if needed!)
//            List<TrackedObject> oldList = waitingList.get(time + frequency);
//            oldList.addAll(newTrackedObjects);
//            waitingList.replace(time + frequency, oldList);
//        }
        //return lastTrackedObjects;
    }

//    public List<TrackedObject> searchTrackedObjects(int currTick){
//        List<TrackedObject> trackedObjectList = new ArrayList<>();
//
//        //search trackedObjects list for relevant objects to send
//        for (TrackedObject object : lastTrackedObjects ){
//
//            //currTick is at least Detection time + lidar_frequency
//            if (currTick >= object.getTime() + frequency){
//                if (object.getId().equals("ERROR")){
//                    System.err.println("[LiDAR Worker " + id + "] ERROR detected: "
//                            + object.getDescription() + ". Terminating service.");
//                    status = STATUS.ERROR;
//                    return null;
//                }
//                trackedObjectList.add(object);
////                    System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Processing object ID: "
////                            + object.getId() + " at tick: " + currTick);
//            }
//        }
//        return trackedObjectList;
//    }

//    public void addTrackedObject (TrackedObject object) {
//        lastTrackedObjects.add(object);
//    }

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
            System.out.println("[LiDAR Worker " + id + "] Sending "
                    + trackedObjectList.size() + " tracked objects. Latest object time: "
                    + trackedObjectList.get(trackedObjectList.size() - 1).getTime());
            lastTrackedObjects.removeAll(trackedObjectList);
        }
        return trackedObjectList;
    }

    public boolean isFinished() {
        return dataBase.isFinished();
    }
}

