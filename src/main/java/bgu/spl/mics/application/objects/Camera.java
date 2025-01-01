package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */

public class Camera {
    private int id;
    private int frequency;
    private STATUS status;
    private List<StampedDetectedObjects> detectedObjectList ;

    public Camera(int id, int frequency) {  //where do we get the status?
        this.id = id;
        this.frequency = frequency;
        status = STATUS.UP;
        detectedObjectList = new ArrayList<>();
    }

    public int getId(){
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public List<StampedDetectedObjects> getDetectedObjectList() {
        return detectedObjectList;
    }

    public void setStatus(STATUS status){
        this.status = status;
    }

    public void addStampedObject (StampedDetectedObjects object){
        detectedObjectList.add(object);
    }
}
