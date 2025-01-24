package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
    private StampedDetectedObjects stampedDetectedObjects;

    public DetectObjectsEvent(StampedDetectedObjects objects){
        stampedDetectedObjects = objects;
    }

    public StampedDetectedObjects getStampedDetectedObjects() {
        return stampedDetectedObjects;
    }
}
