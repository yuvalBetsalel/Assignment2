package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private CountDownLatch latch;
    private GPSIMU gpsimu; //private?
    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu, CountDownLatch latch) {
        super("Pose");
        this.gpsimu = gpsimu;
        this.latch = latch;
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

        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currTick = tick.getCounter();
            if (currTick >= gpsimu.getPoseList().size())
                terminateService();
            else {
                Pose currPose = gpsimu.getPoseList().get(currTick - 1);
                sendEvent(new PoseEvent<>(currPose));
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
