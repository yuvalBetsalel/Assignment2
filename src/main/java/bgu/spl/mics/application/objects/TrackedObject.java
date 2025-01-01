package bgu.spl.mics.application.objects;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description, 
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject {
    private String id;
    private int time;
    private String description;
    private List<CloudPoint> coordinates;

    public TrackedObject(String id, int time, String description){
        this.id = id;
        this.time = time;
        this.description = description;
        coordinates = new ArrayList<>();
    }

    public int getTime() {
        return time;
    }

    public String getId(){
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }

    public void addCloudPoint(CloudPoint cloudPoint){
        coordinates.add(cloudPoint);
    }
}
