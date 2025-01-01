package bgu.spl.mics.application.objects;

import java.util.List;

public class CamerasConfigurations {
    private List<Camera> cameras;
    private String filePath;

    public CamerasConfigurations(List<Camera> cameras, String filePath){
        this.cameras = cameras;
        this.filePath = filePath;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public String getFilePath() {
        return filePath;
    }
}

