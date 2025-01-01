package bgu.spl.mics.application;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;

import static bgu.spl.mics.application.services.FusionSlamService.addCounter;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        String configFilePath = args[0];
        Config config = ConfigParser.parseConfig(configFilePath);
        int index = configFilePath.lastIndexOf("/");
        String filePathLocation = configFilePath.substring(0,index);
        //make new camera services:
        String cameraFilePath = filePathLocation + config.getCamerasConfigurations().getFilePath().substring(1);
        for (Camera camera : config.getCamerasConfigurations().getCameras()){
            CameraService newCameraService = new CameraService(camera);
            newCameraService.setFilePath(cameraFilePath);
            new Thread(newCameraService).start();
            addCounter();
        }
        //make new lidar services:
        String lidarFilePath = filePathLocation + config.getLidarConfigurations().getFilePath().substring(1);
        for (LiDarWorkerTracker lidar : config.getLidarConfigurations().getLidars()){
            LiDarService newLidarService = new LiDarService(lidar);
            newLidarService.setFilePath(lidarFilePath);
            new Thread(newLidarService).start();
            addCounter();
        }
        //make new pose service:
        String poseFilePath = filePathLocation + config.getPoseJsonFile().substring(1);
        PoseService newPoseService = new PoseService(new GPSIMU(poseFilePath));
        new Thread(newPoseService).start();
        addCounter();
        //make new time service:
        TimeService newTimeService = new TimeService(config.getTickTime(), config.getDuration());
        new Thread(newTimeService).start();
        addCounter();
        //make new fusionSlam service:
        FusionSlamService newFutionSlamService = new FusionSlamService(new FusionSlam());
        new Thread(newFutionSlamService).start();
    }



}
