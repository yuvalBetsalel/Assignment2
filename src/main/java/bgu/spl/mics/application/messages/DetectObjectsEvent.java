package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
    private StampedDetectedObjects stampedDetectedObjects;
  /*  private int detectedTime;
    private List<DetectedObject> detectedObjects;
*/
    public DetectObjectsEvent(StampedDetectedObjects objects){
        stampedDetectedObjects = objects;
        //System.out.println("new DetectedObjectsEvent was created");
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
