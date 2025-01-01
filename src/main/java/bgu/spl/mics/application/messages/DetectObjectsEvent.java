package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.List;

public class DetectObjectsEvent<T> implements Event<T> {
    private StampedDetectedObjects stampedDetectedObjects;
  /*  private int detectedTime;
    private List<DetectedObject> detectedObjects;
*/
    public DetectObjectsEvent(StampedDetectedObjects objects){
        stampedDetectedObjects = objects;
        /*detectedTime = time;
        detectedObjects = objects;*/
    }

    public StampedDetectedObjects getStampedDetectedObjects() {
        return stampedDetectedObjects;
    }

    //    public int getDetectedTime() {
//        return detectedTime;
//    }
//
//    public List<DetectedObject> getDetectedObjects() {
//        return detectedObjects;
//    }
}
