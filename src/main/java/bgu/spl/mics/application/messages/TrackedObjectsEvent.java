package bgu.spl.mics.application.messages;

import java.util.List;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

public class TrackedObjectsEvent implements Event<Boolean> {
    private List<TrackedObject> trackedObjects;

    public TrackedObjectsEvent(List<TrackedObject> list){
        trackedObjects = list;
        //System.out.println("new TrackedObjectsEvent was created");
    }

    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }
}
