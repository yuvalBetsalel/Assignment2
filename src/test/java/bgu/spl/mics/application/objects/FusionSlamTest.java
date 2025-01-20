package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamTest {
    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.getLandmarks().clear(); // Clear existing landmarks
        fusionSlam.getPoses().clear(); // Clear existing poses
    }



    /**
     * Test for updateLandMarkCoordinates.
     * @pre A valid LandMark with existing coordinates is added to FusionSlam.
     * @pre A list of new CloudPoints is provided.
     * @post The LandMark's coordinates are updated by averaging with the new CloudPoints.
     * @post If the new CloudPoints list is longer, additional points are appended.
     */
    @Test
    void updateLandMarkCoordinates() {
        // Initialize a landmark with existing coordinates
        LandMark landMark = new LandMark("landmark1", "Test Landmark",
                new ArrayList<>(Arrays.asList(
                        new CloudPoint(2.0, 3.0),
                        new CloudPoint(4.0, 6.0)
                )));
        fusionSlam.addLandMark(landMark);

        // New cloud points to average with
        List<CloudPoint> newCloudPoints = Arrays.asList(
                new CloudPoint(4.0, 7.0),
                new CloudPoint(6.0, 8.0)
        );

        // Execute update
        fusionSlam.updateLandMarkCoordinates(landMark, newCloudPoints);

        // Verify updated coordinates
        List<CloudPoint> updatedCoordinates = landMark.getCoordinates();
        assertEquals(2, updatedCoordinates.size(), "Landmark should have two updated coordinates.");
        assertEquals(3.0, updatedCoordinates.get(0).getX(), 0.001, "X-coordinate of first point should be averaged.");
        assertEquals(5.0, updatedCoordinates.get(0).getY(), 0.001, "Y-coordinate of first point should be averaged.");
        assertEquals(5.0, updatedCoordinates.get(1).getX(), 0.001, "X-coordinate of second point should be averaged.");
        assertEquals(7.0, updatedCoordinates.get(1).getY(), 0.001, "Y-coordinate of second point should be averaged.");
    }

    /**
     * Test for updateLandMarkCoordinates with different-sized cloud points.
     * @pre A valid LandMark with fewer coordinates than the new CloudPoints is added to FusionSlam.
     * @pre A list of CloudPoints longer than the existing LandMark coordinates is provided.
     * @post The LandMark's coordinates are updated by averaging overlapping points.
     * @post New CloudPoints are appended to the LandMark's coordinates.
     */
    @Test
    void updateLandMarkCoordinatesWithExtraCloudPoints() {
        // Initialize a landmark with existing coordinates
        LandMark landMark = new LandMark("landmark2", "Test Landmark",
                new ArrayList<>(Arrays.asList(
                        new CloudPoint(1.0, 1.0)
                )));
        fusionSlam.addLandMark(landMark);

        // New cloud points list is longer
        List<CloudPoint> newCloudPoints = Arrays.asList(
                new CloudPoint(2.0, 2.0),
                new CloudPoint(3.0, 3.0)
        );

        // Execute update
        fusionSlam.updateLandMarkCoordinates(landMark, newCloudPoints);

        // Verify updated coordinates
        List<CloudPoint> updatedCoordinates = landMark.getCoordinates();
        assertEquals(2, updatedCoordinates.size(), "Landmark should have two coordinates after update.");
        assertEquals(1.5, updatedCoordinates.get(0).getX(), 0.001, "X-coordinate of first point should be averaged.");
        assertEquals(1.5, updatedCoordinates.get(0).getY(), 0.001, "Y-coordinate of first point should be averaged.");
        assertEquals(3.0, updatedCoordinates.get(1).getX(), 0.001, "X-coordinate of second point should be added.");
        assertEquals(3.0, updatedCoordinates.get(1).getY(), 0.001, "Y-coordinate of second point should be added.");
    }


    /**
     * Test for createNewLandMark.
     * @pre A valid Pose corresponding to the tracked object's time is added to FusionSlam.
     * @pre A valid TrackedObject with local CloudPoints is provided.
     * @post A new LandMark is created with correctly transformed global coordinates.
     * @post The LandMark's ID and description match the TrackedObject.
     */
    @Test
    void createNewLandMark() {
        // Add a pose to simulate robot's position and orientation
        Pose pose = new Pose(1, 5, 10, 90); // x=5, y=10, yaw=90 degrees
        fusionSlam.addPoses(pose);

        // Create a tracked object with local coordinates
        TrackedObject trackedObject = new TrackedObject("obj1",1 , "Test Object");

        trackedObject.addCloudPoint(new CloudPoint(1.0, 0.0));
        trackedObject.addCloudPoint(new CloudPoint(0.0, 1.0));

        // Execute createNewLandMark
        LandMark newLandMark = fusionSlam.createNewLandMark(1, trackedObject);

        // Verify the new landmark
        assertNotNull(newLandMark, "New landmark should not be null.");
        assertEquals("obj1", newLandMark.getId(), "Landmark ID should match the tracked object's ID.");
        assertEquals("Test Object", newLandMark.getDescription(), "Landmark description should match the tracked object's description.");

        // Verify transformed coordinates
        List<CloudPoint> globalCoordinates = newLandMark.getCoordinates();
        assertEquals(2, globalCoordinates.size(), "Landmark should have two global coordinates.");
        assertEquals(5.0, globalCoordinates.get(0).getX(), 0.001, "X-coordinate of first point should be transformed correctly.");
        assertEquals(11.0, globalCoordinates.get(0).getY(), 0.001, "Y-coordinate of first point should be transformed correctly.");
        assertEquals(4.0, globalCoordinates.get(1).getX(), 0.001, "X-coordinate of second point should be transformed correctly.");
        assertEquals(10.0, globalCoordinates.get(1).getY(), 0.001, "Y-coordinate of second point should be transformed correctly.");
    }

    /**
     * Test for createNewLandMark with missing pose.
     * @pre No Pose corresponding to the tracked object's time is added to FusionSlam.
     * @pre A valid TrackedObject with local CloudPoints is provided.
     * @post No LandMark is created, and null is returned.
     */
    @Test
    void testCreateNewLandMarkWithMissingPose() {
        // Create a tracked object without adding a pose
        TrackedObject trackedObject = new TrackedObject("obj2",1 , "Test Object");

        trackedObject.addCloudPoint(new CloudPoint(1.0, 0.0));
        trackedObject.addCloudPoint(new CloudPoint(0.0, 1.0));

        // Execute createNewLandMark
        LandMark newLandMark = fusionSlam.createNewLandMark(1, trackedObject);

        // Verify no landmark is created
        assertNull(newLandMark, "New landmark should be null when no pose is available.");
    }
}