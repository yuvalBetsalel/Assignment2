package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private String id;
    private String description;
    private List<CloudPoint> coordinates;

    public LandMark(String id, String description){
        this.id = id;
        this.description = description;
        coordinates = new ArrayList<>();
    }

    public LandMark(String id, String description, List<CloudPoint> coordinates){
        this(id, description);
        this.coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setCoordinates(int index, CloudPoint point){
        coordinates.set(index, point);
    }

    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }
}
