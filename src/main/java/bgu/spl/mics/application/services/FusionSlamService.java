package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
        if (serviceCounter == 0)
            fusionSlam.setRunning(false);
    }

    private void terminateService() {
        StatisticalFolder.getInstance().setSystemRuntime(time);
        fusionSlam.generateOutputFile();
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
            // Process each tracked object
            List<TrackedObject> trackedObjectList = tracked.getTrackedObjects();
            for (TrackedObject trackedObject : trackedObjectList ){
                boolean isFound = false;
                if (time == trackedObject.getTime()){
                    // Check if the tracked object corresponds to an existing landmark
                    LandMark existingLandMark = fusionSlam.findMatchingLandMark(trackedObject);
                    if (existingLandMark != null) {
                        // Update the existing landmark with averaged coordinates
                        fusionSlam.updateLandMarkCoordinates(existingLandMark, trackedObject);
                    } else {
                        // Create a new landmark and transform local coordinates to global
                        LandMark newLandMark = fusionSlam.createNewLandMark(time, trackedObject);
                        fusionSlam.addLandMark(newLandMark);
                        //messageBus.complete(tracked, newLandMark);
                        StatisticalFolder.getInstance().addLandmarks();
                    }

                }
            }
        });

        subscribeEvent(PoseEvent.class, pose -> {
           fusionSlam.addPoses(pose.getCurrPose());
           //messageBus.complete(pose, pose.getCurrPose());  //???
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> { // wait for all sensors to send terminated broadcast??
            MicroService m = terminated.getSender();
            if(!(m instanceof TimeService)) {
                removeFromCounter();
                if (serviceCounter == 0) {
                    System.out.println("all services finished");
                    terminateService();
                }
            }
            else
                terminateService();

        });
        subscribeBroadcast(CrashedBroadcast.class, crashed -> {  // wait for all sensors to send terminated broadcast??
            terminateService(); //??
        });
    latch.countDown();
    }


}



