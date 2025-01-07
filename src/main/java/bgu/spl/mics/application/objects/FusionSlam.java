package bgu.spl.mics.application.objects;

import bgu.spl.mics.MessageBusImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected final StatisticalFolder statisticalFolder;


    public FusionSlam(){
        landmarks = new ArrayList<>();
        poses = new ArrayList<>();
        isRunning = true;
        statisticalFolder = StatisticalFolder.getInstance();


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
     * @param trackedObject The tracked object providing the new coordinates.
     */
    public void updateLandMarkCoordinates(LandMark landMark, TrackedObject trackedObject) {
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
     */
    public LandMark createNewLandMark(int time, TrackedObject trackedObject) {
        // Retrieve the robot's pose at the given time from Fusion-SLAM
        Double xRobot = (double) poses.get(time).getX();
        Double yRobot = (double) poses.get(time).getY();
        Double yawRobot = (double) poses.get(time).getYaw();
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

    public void generateOutputFile(){
        // Prepare data to serialize
        Map<String, Object> outputData = new HashMap<>();
        // Add statistics
        outputData.put("systemRuntime", statisticalFolder.getSystemRuntime());
        outputData.put("numDetectedObjects", statisticalFolder.getNumDetectedObjects());
        outputData.put("numTrackedObjects", statisticalFolder.getNumTrackedObjects());
        outputData.put("numLandmarks", statisticalFolder.getNumLandmarks());
        // Add landmarks
        Map<String, Object> newLandmarks = new HashMap<>();
        for (LandMark landmark : landmarks) {
            Map<String, Object> landmarkData = new HashMap<>();
            landmarkData.put("id", landmark.getId());
            landmarkData.put("description", landmark.getDescription());
            landmarkData.put("coordinates", landmark.getCoordinates());
            newLandmarks.put(landmark.getId(), landmarkData);
        }
        outputData.put("landMarks", newLandmarks);
        // Serialize to JSON and write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String outputFilePath = "output_file.json"; // Define file path
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            gson.toJson(outputData, writer);
            System.out.println("Output file generated successfully: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Read the file and print its contents
        try (FileReader reader = new FileReader(outputFilePath)) {
            // Read the contents of the file
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            System.out.println("Content of the output file:");
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
