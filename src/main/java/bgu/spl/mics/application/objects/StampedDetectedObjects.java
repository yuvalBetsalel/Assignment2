package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time;
    private List<DetectedObject> detectedObjects;

    public StampedDetectedObjects(int time){
        this.time = time;
        detectedObjects = new ArrayList<>();
    }

    public StampedDetectedObjects(int time, List<DetectedObject> objects){
        this.time = time;
        detectedObjects = objects;
    }
    public int getTime(){
        return time;
    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    //    public void addDetectedObject(DetectedObject object){
//        detectedObjects.add(object);
//    }

}
