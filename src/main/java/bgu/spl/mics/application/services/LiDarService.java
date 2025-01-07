package bgu.spl.mics.application.services;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
    private CountDownLatch latch;
    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker, CountDownLatch latch) {
        super("liDarWorker"+LiDarWorkerTracker.getId());
        this.liDarWorkerTracker = LiDarWorkerTracker;
        statisticalFolder = StatisticalFolder.getInstance();
        this.latch = latch;
    }

//    public void setFilePath(String filePath){
//        liDarWorkerTracker.setDataFile(filePath);
//    }

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
            List<TrackedObject> trackedObjectList = liDarWorkerTracker.checkData(detected.getStampedDetectedObjects());
            //messageBus.complete(detected, trackedObjectList);
        });
        System.out.println("lidar " + liDarWorkerTracker.getId() + " has subscribed to DetectObjectsEvent");


        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currTick = tick.getCounter();
            List<TrackedObject> trackedObjectList = new ArrayList<>();
            //search trackedObjects list for relevant objects to send
            for (TrackedObject object : liDarWorkerTracker.getLastTrackedObjects()){
                //currTick is at least Detection time + lidar_frequency
                if (currTick >= object.getTime() + liDarWorkerTracker.getFrequency()){
                    if (object.getId().equals("ERROR")){
                        liDarWorkerTracker.setStatus(STATUS.ERROR);
                        sendBroadcast(new CrashedBroadcast(this));
                        terminate();
                    }
                    trackedObjectList.add(object);
                }
            }
            //if trackedObject was detected - send TrackedObjectsEvent
            if (!trackedObjectList.isEmpty()) {
                statisticalFolder.addTrackedObjects();
                messageBus.sendEvent(new TrackedObjectsEvent(trackedObjectList));
            }
            //terminates when: curr time > last object time and all objects are tracked
            if (!liDarWorkerTracker.getLastTrackedObjects().isEmpty()) {
                int lastIndex = liDarWorkerTracker.getLastTrackedObjects().size() - 1;
                if (liDarWorkerTracker.getLastTrackedObjects().get(lastIndex).getTime() + liDarWorkerTracker.getFrequency() < currTick
                        && liDarWorkerTracker.getDataBase().getSize() == liDarWorkerTracker.getLastTrackedObjects().size()) {
                    terminateService();
                }
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
