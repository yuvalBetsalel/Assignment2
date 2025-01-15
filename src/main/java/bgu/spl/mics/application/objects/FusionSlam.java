package bgu.spl.mics.application.objects;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void addPoses(Pose pose) {
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
        for (int i = 0; i < cloudpoints.size(); i++) {
            Double newX = (landMark.getCoordinates().get(i).getX()
                    + cloudpoints.get(i).getX()) / 2;
            Double newY = (landMark.getCoordinates().get(i).getY()
                    + cloudpoints.get(i).getY()) / 2;

            CloudPoint newCloudPoint = new CloudPoint(newX, newY);
            landMark.setCoordinates(i, newCloudPoint);
        }
        if (cloudpoints.size() < cloudpoints.size()){
            for (int i = landMark.getCoordinates().size() ; i < cloudpoints.size(); i++){
                landMark.setCoordinates(i, cloudpoints.get(i));
            }
        }
    }

    public Pose getPose(int time){
        for(Pose p: poses){
            if(p.getTime() == time)
                return p;
        }
        return null;
    }

    /**
     * Creates a new landmark by transforming local coordinates to the global coordinate system.
     *
     * @param trackedObject The tracked object providing the local coordinates.
     */
    public LandMark createNewLandMark(int time, TrackedObject trackedObject) {
        // Retrieve the robot's pose at the given time from Fusion-SLAM
        Pose pose = getPose(time);
        if(pose == null) return null;
        System.out.println("current pose: " + pose + " tracked object: " + trackedObject);
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
        String outputFilePath = baseDirectory + "error_output_Y&H.json"; // Define file path
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(ErrorOutput.getInstance(), writer);
            System.out.println("Error Output file generated successfully: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateOutputFile(){
//        // Prepare data to serialize
//        Map<String, Object> outputData = new HashMap<>();
//        // Add statistics
//        outputData.put("systemRuntime", statisticalFolder.getSystemRuntime());
//        outputData.put("numDetectedObjects", statisticalFolder.getNumDetectedObjects());
//        outputData.put("numTrackedObjects", statisticalFolder.getNumTrackedObjects());
//        outputData.put("numLandmarks", statisticalFolder.getNumLandmarks());
//        // Add landmarks
//        Map<String, Object> newLandmarks = new HashMap<>();
//        for (LandMark landmark : landmarks) {
//            Map<String, Object> landmarkData = new HashMap<>();
//            landmarkData.put("id", landmark.getId());
//            landmarkData.put("description", landmark.getDescription());
//            landmarkData.put("coordinates", landmark.getCoordinates());
//            newLandmarks.put(landmark.getId(), landmarkData);
//        }
//        outputData.put("landMarks", newLandmarks);
        // Serialize to JSON and write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String outputFilePath = baseDirectory + "output_file_Y&H.json"; // Define file path
        System.out.println("saving to " + outputFilePath);
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(statisticalFolder, writer);
            System.out.println("Output file generated successfully: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Read the file and print its contents
//        try (FileReader reader = new FileReader(outputFilePath)) {
//            // Read the contents of the file
//            BufferedReader bufferedReader = new BufferedReader(reader);
//            String line;
//            System.out.println("Content of the output file:");
//            while ((line = bufferedReader.readLine()) != null) {
//                System.out.println(line);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public void process(){
        List<TrackedObject> copyList = new ArrayList<>(awaitingProcess);
        for (TrackedObject trackedObject : copyList ){
            if(getPose(trackedObject.getTime()) == null){
                continue;
            }
            awaitingProcess.remove(trackedObject);
            LandMark existingLandMark = findMatchingLandMark(trackedObject);
            if (existingLandMark != null) {
                // Update the existing landmark with averaged coordinates
                LandMark newLandMark = createNewLandMark(trackedObject.getTime(), trackedObject);
                updateLandMarkCoordinates(existingLandMark, newLandMark.getCoordinates());
            } else {
                // Create a new landmark and transform local coordinates to global
                System.out.println("[FusionSlamService] Creating new landmark for tracked object: " + trackedObject.getId());
                LandMark newLandMark = createNewLandMark(trackedObject.getTime(), trackedObject);
                addLandMark(newLandMark);
                //messageBus.complete(tracked, newLandMark);
                StatisticalFolder.getInstance().addLandmarks();
            }
        }
    }

    public List<TrackedObject> getAwaitingProcess() {
        return awaitingProcess;
    }
}
