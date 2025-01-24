package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    FusionSlam fusionSlam;
    int time;
    private static int serviceCounter;
    private CountDownLatch latch;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam, CountDownLatch latch) {
        super("FusionSlam");
        this.fusionSlam = fusionSlam;
        time = 0;
        serviceCounter = 0;
        this.latch = latch;
    }

    public static void addCounter(){
        serviceCounter++;
    }

    public void removeFromCounter(){
        serviceCounter--;
        if (serviceCounter <= 0)
            fusionSlam.setRunning(false);
    }

    private void terminateService() {
        fusionSlam.setRunning(false);
        StatisticalFolder.getInstance().setSystemRuntime(time);
        StatisticalFolder.getInstance().setLandMarks(fusionSlam.getLandmarks());
        this.terminate(); //microService function
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            time++;
        });

        subscribeEvent(TrackedObjectsEvent.class, tracked -> {
            fusionSlam.getAwaitingProcess().addAll(tracked.getTrackedObjects());
            // Process only if tracked objects are already waiting
            if (!fusionSlam.getAwaitingProcess().isEmpty()) {
                fusionSlam.process();
            }
            if (serviceCounter <= 0 && fusionSlam.getAwaitingProcess().isEmpty()) {
                messageBus.sendBroadcast(new TerminatedBroadcast(this));
                terminateService();
                fusionSlam.generateOutputFile();
            }
        });

        subscribeEvent(PoseEvent.class, pose -> {
            fusionSlam.addPoses(pose.getCurrPose());
            // Process only if tracked objects are already waiting
            if (!fusionSlam.getAwaitingProcess().isEmpty()) {
                fusionSlam.process();
            }
            if (serviceCounter <= 0 && fusionSlam.getAwaitingProcess().isEmpty()) {
                messageBus.sendBroadcast(new TerminatedBroadcast(this));
                terminateService();
                fusionSlam.generateOutputFile();
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            MicroService m = terminated.getSender();
            if((m instanceof CameraService) || (m instanceof LiDarService )) {
                removeFromCounter();
                if (serviceCounter <= 0 && fusionSlam.getAwaitingProcess().isEmpty()) {
                    messageBus.sendBroadcast(new TerminatedBroadcast(this));
                    terminateService();
                    fusionSlam.generateOutputFile();
                }
            } else if (m instanceof TimeService) {
                terminateService();
                fusionSlam.generateOutputFile();
            }

        });
        subscribeBroadcast(CrashedBroadcast.class, crashed -> {
            fusionSlam.setRunning(false);
            terminateService();
            fusionSlam.generateErrorOutput();
        });
        latch.countDown();
    }


}



