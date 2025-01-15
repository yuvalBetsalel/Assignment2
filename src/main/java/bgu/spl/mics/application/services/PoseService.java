package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.ErrorOutput;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private CountDownLatch latch;
    private GPSIMU gpsimu; //private?
    protected final ErrorOutput errorOutput;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu, CountDownLatch latch) {
        super("Pose");
        this.gpsimu = gpsimu;
        this.latch = latch;
        errorOutput = ErrorOutput.getInstance();
    }



    private void terminateService() {
        gpsimu.setStatus(STATUS.DOWN);
        this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
        this.terminate(); //microService function
    }

        /**
         * Initializes the PoseService.
         * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
         */
    @Override
    protected void initialize() {
        gpsimu.loadPoseData();
        System.out.println("[PoseService] Pose data loaded. Total poses: " + gpsimu.getPoseList().size());


        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currTick = tick.getCounter();
            if (currTick >= gpsimu.getPoseList().size())
                terminateService();
            else {
                Pose currPose = gpsimu.getPoseList().get(currTick - 1);
                System.out.println("[PoseService] Broadcasting PoseEvent for pose at tick: " + currTick +
                        " [x=" + currPose.getX() + ", y=" + currPose.getY() + ", yaw=" + currPose.getYaw() + "]");
                errorOutput.setPoses(gpsimu.getPoseList().subList(0,currTick));
                sendEvent(new PoseEvent(currPose));
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            MicroService m = terminated.getSender();
            if (m instanceof TimeService) {
                terminateService();
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, crashed -> {
            terminateService();
        });
        latch.countDown();

    }
}
