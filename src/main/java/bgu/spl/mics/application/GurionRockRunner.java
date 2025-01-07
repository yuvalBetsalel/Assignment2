package bgu.spl.mics.application;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;

import java.util.concurrent.CountDownLatch;

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
        System.out.println("Starting Simulation");
        String configFilePath = "C:/Users/97254/Downloads/Skeleton/example input/configuration_file.json"; //args[0];
        Config config = ConfigParser.parseConfig(configFilePath);
        int index = configFilePath.lastIndexOf("/");
        String filePathLocation = configFilePath.substring(0,index);

        // Total number of services to initialize
        int totalServices = config.getCameras().getCamerasConfigurations().size()
                + config.getLiDarWorkers().getLidarConfigurations().size()
                + 2; // PoseService and FusionSlamService

        // Create a CountDownLatch to synchronize initialization
        CountDownLatch latch = new CountDownLatch(totalServices);

        //make new camera services:
        String cameraFilePath = filePathLocation + config.getCameras().getCamera_datas_path().substring(1);
        for (Camera camera : config.getCameras().getCamerasConfigurations()){
            Camera newCamera = new Camera(camera.getId(), camera.getFrequency());
            CameraService newCameraService = new CameraService(newCamera, latch);
            newCameraService.setFilePath(cameraFilePath);
            new Thread(newCameraService).start();
            addCounter();
            System.out.println("new camera service was created");
        }
        //make new lidar services:
        String lidarFilePath = filePathLocation + config.getLiDarWorkers().getFilePath().substring(1);
        for (LiDarWorkerTracker lidar : config.getLiDarWorkers().getLidarConfigurations()){
            LiDarWorkerTracker newLidar = new LiDarWorkerTracker(lidar.getId(), lidar.getFrequency(), lidarFilePath);
            LiDarService newLidarService = new LiDarService(newLidar, latch);
            //newLidarService.setFilePath(lidarFilePath);
            new Thread(newLidarService).start();
            addCounter();
            System.out.println("new lidar service was created");
        }
        //make new pose service:
        String poseFilePath = filePathLocation + config.getPoseJsonFile().substring(1);
        PoseService newPoseService = new PoseService(new GPSIMU(poseFilePath), latch);
        new Thread(newPoseService).start();
        addCounter();
        System.out.println("new pose service was created");
        //make new time service:
        TimeService newTimeService = new TimeService(config.getTickTime(), config.getDuration(), latch);
        new Thread(newTimeService).start();
        //addCounter();
        System.out.println("new time service was created");
        //make new fusionSlam service:
        FusionSlamService newFusionSlamService = new FusionSlamService(new FusionSlam(), latch);
        new Thread(newFusionSlamService).start();
        //MessageBusImpl.getInstance().setInitializeCounter(numOfCamera + numOfLidar + 3);
        System.out.println("new fusion service was created");

    }
}
