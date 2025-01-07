package bgu.spl.mics.application.objects;

import java.util.List;

public class CamerasConfigurations {
    private List<Camera> cameraList;

    public CamerasConfigurations(List<Camera> cameras){
        cameraList = cameras;
    }

    public List<Camera> getConfCameras() {
        return cameraList;
    }


}

