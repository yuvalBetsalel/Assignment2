package bgu.spl.mics.application;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.Config;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.services.CameraService;
import bgu.spl.mics.application.services.FusionSlamService;
import static bgu.spl.mics.application.services.FusionSlamService.addCounter;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.application.services.PoseService;
import bgu.spl.mics.application.services.TimeService;

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
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        String configFilePath = args[0];
        Config config = ConfigParser.parseConfig(configFilePath);
        String filePathLocation = Paths.get(configFilePath).getParent().toFile().getAbsolutePath() + File.separator;
        FusionSlam.getInstance().setBaseDirectory(filePathLocation);

        // Total number of services to initialize
        int totalServices = config.getCameras().getCamerasConfigurations().size()
                + config.getLiDarWorkers().getLidarConfigurations().size()
                + 2; // PoseService and FusionSlamService

        // Create a CountDownLatch to synchronize initialization
        CountDownLatch latch = new CountDownLatch(totalServices);

        // Make new fusionSlam service:
        FusionSlamService newFusionSlamService = new FusionSlamService(FusionSlam.getInstance(), latch);
        new Thread(newFusionSlamService).start();

        // Make new camera services:
        String cameraFilePath = filePathLocation + config.getCameras().getCamera_datas_path().substring(1);
        for (Camera camera : config.getCameras().getCamerasConfigurations()){
            Camera newCamera = new Camera(camera.getId(), camera.getFrequency());
            CameraService newCameraService = new CameraService(newCamera, latch);
            newCameraService.setFilePath(cameraFilePath);
            addCounter();
            new Thread(newCameraService).start();
        }
        // Make new lidar services:
        String lidarFilePath = filePathLocation + config.getLiDarWorkers().getFilePath().substring(1);
        for (LiDarWorkerTracker lidar : config.getLiDarWorkers().getLidarConfigurations()){
            LiDarWorkerTracker newLidar = new LiDarWorkerTracker(lidar.getId(), lidar.getFrequency(), lidarFilePath);
            LiDarService newLidarService = new LiDarService(newLidar, latch);
            addCounter();
            new Thread(newLidarService).start();

        }
        // Make new pose service:
        String poseFilePath = filePathLocation + config.getPoseJsonFile().substring(1);
        PoseService newPoseService = new PoseService(new GPSIMU(poseFilePath), latch);
        new Thread(newPoseService).start();
        // Make new time service:
        TimeService newTimeService = new TimeService(config.getTickTime(), config.getDuration(), latch);
        new Thread(newTimeService).start();
    }
}
