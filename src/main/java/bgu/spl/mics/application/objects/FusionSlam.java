package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    // Singleton instance holder
    private static class FusionSlamHolder {
        private static FusionSlam instance = new FusionSlam();
    }
    private List<LandMark> landmarks;
    private List<Pose> poses;
    private boolean isRunning;
    private List<TrackedObject> awaitingProcess;
    private String baseDirectory;
    protected final StatisticalFolder statisticalFolder;


    private FusionSlam(){
        landmarks = new ArrayList<>();
        poses = new ArrayList<>();
        isRunning = true;
        statisticalFolder = StatisticalFolder.getInstance();
        awaitingProcess = new ArrayList<>();
        baseDirectory = "";
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public static FusionSlam getInstance(){
        return FusionSlamHolder.instance;
    }

    public List<LandMark> getLandmarks() {
        return landmarks;
    }

    public List<Pose> getPoses() {
        return poses;
    }

    public List<TrackedObject> getAwaitingProcess() {
        return awaitingProcess;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void addPoses(Pose pose) {
        if (pose == null || pose.getTime() < 0) {
            throw new IllegalArgumentException("Invalid pose: " + pose);
        }
        poses.add(pose);
    }
    public void addLandMark(LandMark landMark){
        landmarks.add(landMark);
    }

    /**
     * Finds a matching landmark for the given tracked object based on ID.
     *
     * @param trackedObject The tracked object to match.
     * @return The matching landmark, or null if no match is found.
     */
    public LandMark findMatchingLandMark(TrackedObject trackedObject) {
        for (LandMark landMark : landmarks) {
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
     * @param cloudpoints The cloud points of the tracked object providing the new coordinates.
     */
    public void updateLandMarkCoordinates(LandMark landMark, List<CloudPoint> cloudpoints) {
        // Ensure landMarkCoordinates has enough capacity
        while (landMark.getCoordinates().size() < cloudpoints.size()) {
            landMark.addCoordinates(null); // Add placeholder values
        }

        // Update existing points
        for (int i = 0; i <cloudpoints.size(); i++) {
            if (landMark.getCoordinates().get(i) == null) {
                // If the existing coordinate is null, simply add the cloud point
                landMark.setCoordinates(i, cloudpoints.get(i));
            } else {
                // Otherwise, average the coordinates
                Double newX = (landMark.getCoordinates().get(i).getX() + cloudpoints.get(i).getX()) / 2;
                Double newY = (landMark.getCoordinates().get(i).getY() + cloudpoints.get(i).getY()) / 2;

                CloudPoint newCloudPoint = new CloudPoint(newX, newY);
                landMark.setCoordinates(i, newCloudPoint);
            }
        }
    }

    public Pose getPose(int time){
        for(Pose p: poses){
            if (p.getTime() == time) {
                return p;
            }
        }
        return null;
    }

    /**
     * Creates a new landmark by transforming local coordinates to the global coordinate system.
     * @param trackedObject The tracked object providing the local coordinates.
     */
    public LandMark createNewLandMark(int time, TrackedObject trackedObject) {
        // Retrieve the robot's pose at the given time from Fusion-SLAM
        Pose pose = getPose(time);
        if (pose == null){
            return null;
        }
        Double xRobot = (double) pose.getX();
        Double yRobot = (double) pose.getY();
        Double yawRobot = (double) pose.getYaw();
        yawRobot = Math.toRadians(yawRobot);
        Double cosYaw = Math.cos(yawRobot);
        Double sinYaw = Math.sin(yawRobot);
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

    public void generateErrorOutput(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String outputFilePath = baseDirectory + "error_output.json"; // Define file path
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(ErrorOutput.getInstance(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateOutputFile(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String outputFilePath = baseDirectory + "output_file.json"; // Define file path
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(statisticalFolder, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void process(){
        List<TrackedObject> copyList = new ArrayList<>(awaitingProcess);
        for (TrackedObject trackedObject : copyList ){
            Pose pose = getPose(trackedObject.getTime());
            if (pose == null){
                continue;
            }
            awaitingProcess.remove(trackedObject);
            LandMark existingLandMark = findMatchingLandMark(trackedObject);
            if (existingLandMark != null) {
                // Update the existing landmark with averaged coordinates
                LandMark newLandMark = createNewLandMark(trackedObject.getTime(), trackedObject);
                updateLandMarkCoordinates(existingLandMark, newLandMark.getCoordinates());
            } else {
                LandMark newLandMark = createNewLandMark(trackedObject.getTime(), trackedObject);
                addLandMark(newLandMark);
                StatisticalFolder.getInstance().addLandmarks();
            }
        }
    }
}
