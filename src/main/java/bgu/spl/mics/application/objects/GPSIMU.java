package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick;  //what is this for?
    private STATUS status;
    private List<Pose> poseList;
    private String filePath;

    public GPSIMU(String filePath){
        currentTick = 0;
        status = STATUS.UP;
        poseList = new ArrayList<>();
        this.filePath = filePath;
    }

    public void add(Pose pose){
        poseList.add(pose);
    }

    public List<Pose> getPoseList(){
        return poseList;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public void loadPoseData() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type poseDataType = new TypeToken<List<Pose>>() {}.getType();
            List<Pose> poseData = gson.fromJson(reader, poseDataType);
            for (Pose currPose : poseData){
                poseList.add(currPose);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
