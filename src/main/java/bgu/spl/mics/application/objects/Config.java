package bgu.spl.mics.application.objects;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Config {
    private Cameras Cameras;
    private LiDarWorkers LiDarWorkers;
    private String poseJsonFile;
    private int TickTime;
    private int Duration;

    public int getDuration() {
        return Duration;
    }

    public int getTickTime() {
        return TickTime;
    }

    public Cameras getCameras() {
        return Cameras;
    }

    public String getPoseJsonFile() {
        return poseJsonFile;
    }

    public LiDarWorkers getLiDarWorkers() {
        return LiDarWorkers;
    }
}


