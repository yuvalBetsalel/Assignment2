package bgu.spl.mics.application.services;

import org.junit.jupiter.api.Test;
import bgu.spl.mics.Message;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import static org.junit.jupiter.api.Assertions.*;

class CameraServiceTest {
    Camera camera;

    @BeforeEach
    void setUp() {
        //List<StampedDetectedObjects> stampedDetectedObjectsList = new ArrayList<>();
//        stampedDetectedObjectsList = new ArrayList<>();
//        stampedDetectedObjectsList.add(new StampedDetectedObjects(1, Arrays.asList(new DetectedObject("Object1", "Description1"))));
//        stampedDetectedObjectsList.add(new StampedDetectedObjects(2, Arrays.asList(new DetectedObject("Object2", "Description2"), new DetectedObject("Object3", "Description3") )));
//        stampedDetectedObjectsList.add(new StampedDetectedObjects(3, Arrays.asList(new DetectedObject("Object4", "Description4"))));

        // Initialize the Camera object
        camera = new Camera(1,0);
    }

    /**
     * @pre:
     *  - The Camera object (camera) is properly initialized.
     *  - The stampedDetectedObjectsList in the Camera object contains valid StampedDetectedObjects entries.
     *  - The input tick (2 in this case) is a valid integer and corresponds to a time that may have detected objects.
     * @post:
     *  - The returned list contains all detected objects at the specified tick if the tick exists in stampedDetectedObjectsList.
     *  - The size of the returned list equals the number of detected objects associated with the given tick.
     *  - The counter in the Camera object is incremented upon the method call.
     * @inv:
     *  - The detectedObjectsList in the Camera object remains unchanged.
     *  - The method does not modify the state of the objects in the detectedObjectsList.
     *  - The Camera object's internal state is consistent before and after the method invocation.
     */
    @Test
    void testGetStampedDetectedObjects() {
        // Check for tick = 2
        StampedDetectedObjects stampedDetectedObjects = camera.getDetectedObjectList().get(2);

        // Verify stampedDetectedObjects is not null
        assertNotNull(stampedDetectedObjects);

        List<DetectedObject> detectedObjects = stampedDetectedObjects.getDetectedObjects();

        // Verify the result contains the expected objects
        assertNotNull(detectedObjects);
        assertEquals(2, detectedObjects.size());

        // Verify the details of the first detected object
        assertEquals("Object2", detectedObjects.get(0).getId());
        assertEquals("Description2", detectedObjects.get(0).getDescription());

        // Verify the details of the second detected object
        assertEquals("Object3", detectedObjects.get(1).getId());
        assertEquals("Description3", detectedObjects.get(1).getDescription());
    }

    /**
     * @pre:
     *  - The Camera object (camera) is properly initialized with a detectedObjectsList containing valid StampedDetectedObjects entries.
     *  - The input tick (4 in this case) is a valid integer.
     * @post:
     *  - The returned value is NULL since there are no stamped detected objects for tick = 4.

     * @inv:
     *  - The detectedObjectsList in the Camera object remains unchanged.
     *  - The method does not modify the state of the objects in the StampedDetectedObjectsList.
     *  - The Camera object's internal state is consistent before and after the method invocation.
     */
    @Test
    void testGetStampedDetectedObjectsAtNonExistentTick() {
        // Check for tick = 4 where no detected objects exist
        StampedDetectedObjects stampedDetectedObjects = camera.getDetectedObjectList().get(4);

        // Verify the result is null
        assertNull(stampedDetectedObjects);
    }

    @Test
    void initialize() {
    }
}