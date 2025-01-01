package bgu.spl.mics.application.objects;

/**
 * Represents the robot's pose (position and orientation) in the environment.
 * Includes x, y coordinates and the yaw angle relative to a global coordinate system.
 */
public class Pose {
    private int time;
    private float x;
    private float y;
    private float yaw;

    public Pose(int time, float x, float y, float yaw){
        this.time = time;
        this.x = x;
        this.y = y;
        this.yaw= yaw;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getYaw() {
        return yaw;
    }
}
