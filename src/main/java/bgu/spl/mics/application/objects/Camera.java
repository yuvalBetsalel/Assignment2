package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */

public class Camera {
    private int id;
    private int frequency;
    private String camera_key;
    private STATUS status;
    private List<StampedDetectedObjects> detectedObjectList ;

    public Camera(int id, int frequency) {  //where do we get the status?
        this.id = id;
        this.frequency = frequency;
        camera_key = "camera"+id;
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

    public void setDetectedObjectList(List<StampedDetectedObjects> detectedObjectList) {
        this.detectedObjectList = detectedObjectList;
    }

    public String getCamera_key() {
        return camera_key;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status){
        this.status = status;
    }

    public void addStampedObject (StampedDetectedObjects object){
        detectedObjectList.add(object);
    }

    public void loadCameraData(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type cameraDataType = new TypeToken<Map<String, List<StampedDetectedObjects>>>() {}.getType();
            Map<String, List<StampedDetectedObjects>> cameraData = gson.fromJson(reader, cameraDataType);
            for (Map.Entry<String, List<StampedDetectedObjects>> entry : cameraData.entrySet()){
                String cameraId = entry.getKey();
                if (cameraId.equals("camera"+id)){
                    List<StampedDetectedObjects> stampedObjects = entry.getValue();
                    for (StampedDetectedObjects stampedObj : stampedObjects) {
                        detectedObjectList.add(stampedObj);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
