package bgu.spl.mics.application.objects;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Config {
    private CamerasConfigurations camerasConfigurations;
    private LidarConfigurations lidarConfigurations;
    private String poseJsonFile;
    private int tickTime;
    private int duration;

    public int getDuration() {
        return duration;
    }

    public int getTickTime() {
        return tickTime;
    }

    public CamerasConfigurations getCamerasConfigurations() {
        return camerasConfigurations;
    }

    public String getPoseJsonFile() {
        return poseJsonFile;
    }

    public LidarConfigurations getLidarConfigurations() {
        return lidarConfigurations;
    }


}


