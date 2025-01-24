package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    private static LiDarDataBase instance = null;
    private List<StampedCloudPoints> stampedCloudPoints;
    private int trackedPoints;

    private LiDarDataBase(String filePath){
            stampedCloudPoints = new ArrayList<>();
            loadLidarData(filePath);
            trackedPoints = 0;
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     * @return The singleton instance of LiDarDataBase.
     */
    public static LiDarDataBase getInstance(String filePath) {
        if (instance == null){
            instance = new LiDarDataBase(filePath);
        }
        return instance;
    }

    public List<StampedCloudPoints> getStampedCloudPoints() {
        return stampedCloudPoints;
    }

    private void loadLidarData(String filePath){
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type LidarDataType = new TypeToken<List<StampedCloudPoints>>() {}.getType();
            List<StampedCloudPoints> lidarData = gson.fromJson(reader, LidarDataType);
            for (StampedCloudPoints stampedObj : lidarData) {
                stampedCloudPoints.add(stampedObj);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFinished(){
        return trackedPoints == stampedCloudPoints.size();
    }

    public synchronized void incTracked(){
        trackedPoints++;
    }
}
