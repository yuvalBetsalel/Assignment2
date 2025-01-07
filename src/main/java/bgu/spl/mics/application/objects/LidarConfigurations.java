package bgu.spl.mics.application.objects;

import java.util.List;

public class LidarConfigurations {
    private List<LiDarWorkerTracker> lidars;

    public LidarConfigurations(List<LiDarWorkerTracker> lidars, String filePath){
        this.lidars = lidars;
    }

    public List<LiDarWorkerTracker> getLidars() {
        return lidars;
    }


}
