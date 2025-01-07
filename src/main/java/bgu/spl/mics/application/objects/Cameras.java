package bgu.spl.mics.application.objects;

import java.util.List;

public class Cameras {
    private List<Camera> CamerasConfigurations;
    private String camera_datas_path;

    public Cameras(List<Camera> cameras, String args){
        CamerasConfigurations = cameras;
        camera_datas_path = args;
    }

    public String getCamera_datas_path() {
        return camera_datas_path;
    }

    public List<Camera> getCamerasConfigurations() {
        return CamerasConfigurations;
    }
}
