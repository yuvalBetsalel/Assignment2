package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private Camera camera;
    private StampedDetectedObjects stampedDetectedObject;

    @BeforeEach
    void setUp() {
        camera = new Camera(1, 5); // Example camera initialization
    }

    /**
     * Test for adding multiple stamped objects and checking the list.
     * @pre The camera's detectedObjectList is initially empty.
     * @post The detectedObjectList contains all added objects, in the correct order.
     */
    @Test
    void addStampedObject() {
        // @pre
        assertTrue(camera.getDetectedObjectList().isEmpty(), "Initial detectedObjectList should be empty.");

        // Add multiple objects
        StampedDetectedObjects obj1 = new StampedDetectedObjects(1, new ArrayList<>());
        StampedDetectedObjects obj2 = new StampedDetectedObjects(2, new ArrayList<>());
        StampedDetectedObjects obj3 = new StampedDetectedObjects(3, new ArrayList<>());

        camera.addStampedObject(obj1);
        camera.addStampedObject(obj2);
        camera.addStampedObject(obj3);
        // @post
        List<StampedDetectedObjects> detectedList = camera.getDetectedObjectList();
        assertEquals(3, detectedList.size(), "detectedObjectList should contain three objects.");
        assertEquals(obj1, detectedList.get(0), "First object should match the first added object.");
        assertEquals(obj2, detectedList.get(1), "Second object should match the second added object.");
        assertEquals(obj3, detectedList.get(2), "Third object should match the third added object.");
    }

    /**
     * Test for loading camera data from a file and verifying the list.
     * Includes edge cases like empty detected objects and duplicate times.
     * @pre Valid JSON file path is provided with stamped objects for the current camera ID.
     * @post The detectedObjectList is populated with the correct data.
     */
    @Test
    void loadCameraData() throws IOException {
        // Create a temporary JSON file for testing
        String filePath = "camera_data.json";
        try (FileWriter writer = new FileWriter(filePath)) {
            String jsonData = "{\n" +
                    "  \"camera1\": [\n" +
                    "    {\"time\": 1, \"detectedObjects\": [{\"id\": \"obj1\", \"description\": \"object 1\"}]},\n" +
                    "    {\"time\": 1, \"detectedObjects\": [{\"id\": \"obj2\", \"description\": \"object 2\"}]},\n" +
                    "    {\"time\": 2, \"detectedObjects\": []}\n" +
                    "  ]\n" +
                    "}";
            writer.write(jsonData);
        }

        // @pre
        assertTrue(camera.getDetectedObjectList().isEmpty(), "Initial detectedObjectList should be empty.");

        // Execute
        camera.loadCameraData(filePath);

        // @post
        List<StampedDetectedObjects> detectedList = camera.getDetectedObjectList();
        assertEquals(3, detectedList.size(), "detectedObjectList should contain three entries.");
        assertEquals(1, detectedList.get(0).getTime(), "First entry should have time 1.");
        assertEquals(1, detectedList.get(1).getTime(), "Second entry should have time 1 (duplicate time).");
        assertEquals(2, detectedList.get(2).getTime(), "Third entry should have time 2.");
        assertTrue(detectedList.get(2).getDetectedObjects().isEmpty(), "Third entry should have an empty detected objects list.");
    }
}
