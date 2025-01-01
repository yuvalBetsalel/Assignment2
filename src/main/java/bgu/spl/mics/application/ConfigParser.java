package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.Config;
import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;

public class ConfigParser {
    public static Config parseConfig(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
