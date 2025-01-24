package bgu.spl.mics.application.services;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.*;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    LiDarWorkerTracker liDarWorkerTracker;
    protected final StatisticalFolder statisticalFolder;
    protected final ErrorOutput errorOutput;
    private CountDownLatch latch;
    private int currTick;

    /**
     * Constructor for LiDarService.
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker, CountDownLatch latch) {
        super("liDarWorker"+LiDarWorkerTracker.getId());
        this.liDarWorkerTracker = LiDarWorkerTracker;
        statisticalFolder = StatisticalFolder.getInstance();
        errorOutput = ErrorOutput.getInstance();
        this.latch = latch;
        currTick = 0;
    }

    private void terminateService() {
        liDarWorkerTracker.setStatus(STATUS.DOWN);
        this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
        this.terminate(); //microService function
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        subscribeEvent(DetectObjectsEvent.class, detected -> {
            liDarWorkerTracker.checkData(detected.getStampedDetectedObjects());
            //checks for objects to send:
            List<TrackedObject> readyToSendTrackedObjects = liDarWorkerTracker.canSendTrackedObjects(currTick);
            if (readyToSendTrackedObjects == null){
                sendBroadcast(new CrashedBroadcast(this));
                terminate();
                return;
            }
            if (!readyToSendTrackedObjects.isEmpty()) {
                statisticalFolder.addTrackedObjects(readyToSendTrackedObjects.size());
                errorOutput.addLidarFrame(readyToSendTrackedObjects, "LidarWorkerTracker"+liDarWorkerTracker.getId());
                sendEvent(new TrackedObjectsEvent(readyToSendTrackedObjects));

            }
        });

        subscribeBroadcast(TickBroadcast.class, tick -> {
            currTick = tick.getCounter();
            List<TrackedObject> readyToSendTrackedObjects = liDarWorkerTracker.canSendTrackedObjects(currTick);
            if (readyToSendTrackedObjects == null){
                sendBroadcast(new CrashedBroadcast(this));
                terminate();
                return;
            }
            if (!readyToSendTrackedObjects.isEmpty()) {
                statisticalFolder.addTrackedObjects(readyToSendTrackedObjects.size());
                errorOutput.addLidarFrame(readyToSendTrackedObjects, "LidarWorkerTracker"+liDarWorkerTracker.getId());
                sendEvent(new TrackedObjectsEvent(readyToSendTrackedObjects));
            }
            if (liDarWorkerTracker.getLastTrackedObjects().isEmpty()&& liDarWorkerTracker.isFinished()) {
                terminateService();
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
