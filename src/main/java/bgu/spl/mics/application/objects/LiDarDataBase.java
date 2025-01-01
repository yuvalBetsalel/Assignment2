package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {


    private static class LiDarDataBaseHolder {
        private static LiDarDataBase instance = new LiDarDataBase();
    }
    private static String filePath ;
    private List<StampedCloudPoints> stampedCloudPoints;

    private LiDarDataBase(){
            filePath = "";
            stampedCloudPoints = new ArrayList<>();
            loadLidarData();
    }

    public static void setFilePath(String filePath) {
        LiDarDataBase.filePath = filePath;
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @return The singleton instance of LiDarDataBase.
     */


    public static LiDarDataBase getInstance() {
        return LiDarDataBaseHolder.instance;
    }

    public List<StampedCloudPoints> getStampedCloudPoints() {
        return stampedCloudPoints;
    }

    public int getSize(){
        return stampedCloudPoints.size();
    }

    private void loadLidarData(){
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
}
