package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.List;

public class TrackedObjectsEvent<T> implements Event<T> {
    private List<TrackedObject> trackedObjects;

    public TrackedObjectsEvent(List<TrackedObject> list){
        trackedObjects = list;
    }

    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }
}
