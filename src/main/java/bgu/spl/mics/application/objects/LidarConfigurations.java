package bgu.spl.mics.application.objects;

import java.util.List;

public class LidarConfigurations {
    private List<LiDarWorkerTracker> lidars;
    private String filePath;

    public LidarConfigurations(List<LiDarWorkerTracker> lidars, String filePath){
        this.lidars = lidars;
        this.filePath = filePath;
    }

    public List<LiDarWorkerTracker> getLidars() {
        return lidars;
    }

    public String getFilePath() {
        return filePath;
    }
}
