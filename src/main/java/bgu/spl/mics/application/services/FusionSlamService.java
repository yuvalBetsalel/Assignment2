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
    protected final StatisticalFolder statisticalFolder;
    private static int serviceCounter;
    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlam");
        this.fusionSlam = fusionSlam;
        time = 0;
        statisticalFolder = StatisticalFolder.getInstance();
        serviceCounter = 0;
    }

    public static void addCounter(){
        serviceCounter++;
    }

    public void removeFromCounter(){
        serviceCounter--;
    }
    private void terminateService() {
        statisticalFolder.setSystemRuntime(time);
        generateOutputFile();
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
            // Retrieve the robot's pose at the given time from Fusion-SLAM
            Double xRobot = (double) fusionSlam.getPoses().get(time).getX();
            Double yRobot = (double) fusionSlam.getPoses().get(time).getY();
            Double yawRobot = (double) fusionSlam.getPoses().get(time).getYaw();
            yawRobot = Math.toRadians(yawRobot);
            Double cosYaw = Math.cos(yawRobot);
            Double sinYaw = Math.sin(yawRobot);
            // Process each tracked object
            List<TrackedObject> trackedObjectList = tracked.getTrackedObjects();
            for (TrackedObject trackedObject : trackedObjectList ){
                boolean isFound = false;
                if (time == trackedObject.getTime()){
                    // Check if the tracked object corresponds to an existing landmark
                    LandMark existingLandMark = findMatchingLandMark(trackedObject);
                    if (existingLandMark != null) {
                        // Update the existing landmark with averaged coordinates
                        updateLandMarkCoordinates(existingLandMark, trackedObject);
                    } else {
                        // Create a new landmark and transform local coordinates to global
                        LandMark newLandMark = createNewLandMark(trackedObject, xRobot, yRobot, cosYaw, sinYaw);
                        fusionSlam.addLandMark(newLandMark);
                        messageBus.complete(tracked, newLandMark);
                        statisticalFolder.addLandmarks();
                    }

                }
            }
        });

        subscribeEvent(PoseEvent.class, pose -> {
           fusionSlam.addPoses(pose.getCurrPose());
           messageBus.complete(pose, pose.getCurrPose());  //???
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> { // wait for all sensors to send terminated broadcast??
            removeFromCounter();
            if (serviceCounter == 0){
                terminateService();
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, crashed -> {  // wait for all sensors to send terminated broadcast??
            terminateService(); //??
        });

    }

    /**
     * Finds a matching landmark for the given tracked object based on ID.
     *
     * @param trackedObject The tracked object to match.
     * @return The matching landmark, or null if no match is found.
     */
    private LandMark findMatchingLandMark(TrackedObject trackedObject) {
        for (LandMark landMark : fusionSlam.getLandmarks()) {
            if (trackedObject.getId().equals(landMark.getId())) {
                return landMark;
            }
        }
        return null;
    }

    /**
     * Updates the coordinates of an existing landmark by averaging with a tracked object.
     *
     * @param landMark      The landmark to update.
     * @param trackedObject The tracked object providing the new coordinates.
     */
    private void updateLandMarkCoordinates(LandMark landMark, TrackedObject trackedObject) {
        for (int i = 0; i < landMark.getCoordinates().size(); i++) {
            Double newX = (landMark.getCoordinates().get(i).getX()
                    + trackedObject.getCoordinates().get(i).getX()) / 2;
            Double newY = (landMark.getCoordinates().get(i).getY()
                    + trackedObject.getCoordinates().get(i).getY()) / 2;

            CloudPoint newCloudPoint = new CloudPoint(newX, newY);
            landMark.setCoordinates(i, newCloudPoint);
        }
        if (landMark.getCoordinates().size() < trackedObject.getCoordinates().size()){
            for (int i = landMark.getCoordinates().size() ; i < trackedObject.getCoordinates().size(); i++){
                landMark.setCoordinates(i, trackedObject.getCoordinates().get(i));
            }
        }
    }

    /**
     * Creates a new landmark by transforming local coordinates to the global coordinate system.
     *
     * @param trackedObject The tracked object providing the local coordinates.
     * @param xRobot        The robot's global x-coordinate.
     * @param yRobot        The robot's global y-coordinate.
     * @param cosYaw        The cosine of the robot's yaw angle.
     * @param sinYaw        The sine of the robot's yaw angle.
     */
    private LandMark createNewLandMark(TrackedObject trackedObject, Double xRobot, Double yRobot, Double cosYaw, Double sinYaw) {
        List<CloudPoint> newCoordinates = new ArrayList<>();

        for (CloudPoint localCloudPoint : trackedObject.getCoordinates()) {
            Double xLocal = localCloudPoint.getX();
            Double yLocal = localCloudPoint.getY();
            Double xGlobal = (cosYaw * xLocal) - (sinYaw * yLocal) + xRobot;
            Double yGlobal = (sinYaw * xLocal) + (cosYaw * yLocal) + yRobot;
            CloudPoint globalCloudPoint = new CloudPoint(xGlobal, yGlobal);
            newCoordinates.add(globalCloudPoint);
        }
        return new LandMark(trackedObject.getId(), trackedObject.getDescription(), newCoordinates);
    }

    public void generateOutputFile(){
        // Prepare data to serialize
        Map<String, Object> outputData = new HashMap<>();
        // Add statistics
        outputData.put("systemRuntime", statisticalFolder.getSystemRuntime());
        outputData.put("numDetectedObjects", statisticalFolder.getNumDetectedObjects());
        outputData.put("numTrackedObjects", statisticalFolder.getNumTrackedObjects());
        outputData.put("numLandmarks", statisticalFolder.getNumLandmarks());
        // Add landmarks
        Map<String, Object> landmarks = new HashMap<>();
        for (LandMark landmark : fusionSlam.getLandmarks()) {
            Map<String, Object> landmarkData = new HashMap<>();
            landmarkData.put("id", landmark.getId());
            landmarkData.put("description", landmark.getDescription());
            landmarkData.put("coordinates", landmark.getCoordinates());
            landmarks.put(landmark.getId(), landmarkData);
        }
        outputData.put("landMarks", landmarks);
        // Serialize to JSON and write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("output_file.json")) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



