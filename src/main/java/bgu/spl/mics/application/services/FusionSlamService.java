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
        //this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
        fusionSlam.setRunning(false);
        StatisticalFolder.getInstance().setSystemRuntime(time);
        StatisticalFolder.getInstance().setLandMarks(fusionSlam.getLandmarks());
        fusionSlam.generateOutputFile();
        System.out.println("[FusionSlamService] Output file generated successfully.");
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
//            System.out.println("[DEBUG] Received TrackedObjectsEvent for time: " + tracked.getTrackedObjects().get(0).getTime());
            fusionSlam.getAwaitingProcess().addAll(tracked.getTrackedObjects());
            // Process only if tracked objects are already waiting
            if (!fusionSlam.getAwaitingProcess().isEmpty()) {
                fusionSlam.process();
            }
            //fusionSlam.process();
        });

        subscribeEvent(PoseEvent.class, pose -> {
//            System.out.println("[DEBUG] Handling PoseEvent for time: " + pose.getCurrPose().getTime());
            fusionSlam.addPoses(pose.getCurrPose());
            // Process only if tracked objects are already waiting
            if (!fusionSlam.getAwaitingProcess().isEmpty()) {
                fusionSlam.process();
            }
            //fusionSlam.process();
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> { // wait for all sensors to send terminated broadcast??
            MicroService m = terminated.getSender();
            if((m instanceof CameraService) || (m instanceof LiDarService )) {
                removeFromCounter();
                if (serviceCounter <= 0) {
                    System.out.println("[FusionSlamService] All services finished. Terminating Fusion SLAM Service.");
                    messageBus.sendBroadcast(new TerminatedBroadcast(this));
                    terminateService();
                }
            } else if (m instanceof TimeService) {
                System.out.println("[FusionSlamService] Received TerminatedBroadcast from TimeService. Terminating Fusion SLAM Service.");
                terminateService();
            }

        });
        subscribeBroadcast(CrashedBroadcast.class, crashed -> {  // wait for all sensors to send terminated broadcast??
            System.err.println("[FusionSlamService] Received CrashedBroadcast. Terminating Fusion SLAM Service.");
            fusionSlam.setRunning(false);
            fusionSlam.generateErrorOutput();
            this.terminate(); //microService function
        });
    latch.countDown();
    }


}



