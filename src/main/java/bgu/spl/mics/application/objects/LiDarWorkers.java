package bgu.spl.mics.application.objects;

import java.util.List;

public class LiDarWorkers {
    private List<LiDarWorkerTracker> LidarConfigurations;
    private String lidars_data_path;

    public LiDarWorkers(List<LiDarWorkerTracker> lidars, String args){
        LidarConfigurations = lidars;
        lidars_data_path = args;
    }

    public List<LiDarWorkerTracker> getLidarConfigurations() {
        return LidarConfigurations;
    }

    public String getFilePath() {
        return lidars_data_path;
    }

}
