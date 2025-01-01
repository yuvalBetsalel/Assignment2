package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent<T> implements Event<T> {
    private Pose currPose;

    public PoseEvent(Pose pose){
        currPose = pose;
    }

    public Pose getCurrPose() {
        return currPose;
    }
}
