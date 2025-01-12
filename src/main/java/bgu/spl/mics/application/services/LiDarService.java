package bgu.spl.mics.application.services;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;

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
    private int currTick;
    //private Map<Integer, List<TrackedObject>> waitingList;

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
        //waitingList = new ConcurrentHashMap<>();
        currTick = 0;
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
            System.out.println("[" + getName() + "]" + " received detected objects of time " + detected.getStampedDetectedObjects().getTime());
            liDarWorkerTracker.checkData(detected.getStampedDetectedObjects());
        });


            // Retrieve tracked objects and add to the waiting list
//            int numOfTracked = liDarWorkerTracker.checkData(detected.getStampedDetectedObjects()); //delete numOfTrucked
//            if (numOfTracked == -1){
//                sendBroadcast(new CrashedBroadcast(this));
//                terminate();
//            }
//            System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Detected "
//                            + numOfTracked + " objects at time: "
//                            + detected.getStampedDetectedObjects().getTime());
//
//            //if ready to send tracked object
//
//
//            if (liDarWorkerTracker.getWaitingList().containsKey(lastTick)){
//                List<TrackedObject> trackedObjectList = liDarWorkerTracker.getWaitingList().get(lastTick);
//                statisticalFolder.addTrackedObjects();
//                System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Sending "
//                        + trackedObjectList.size() + " tracked objects. Latest object time: "
//                        + trackedObjectList.get(trackedObjectList.size() - 1).getTime());
//                messageBus.sendEvent(new TrackedObjectsEvent(trackedObjectList));
//            }
//        });


        subscribeBroadcast(TickBroadcast.class, tick -> {
            currTick = tick.getCounter();
            System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Tick: " + currTick);
            List<TrackedObject> readyToSendTrackedObjects = liDarWorkerTracker.canSendTrackedObjects(currTick);
            if (readyToSendTrackedObjects == null){
                sendBroadcast(new CrashedBroadcast(this));
                terminate();
                return;
            }
            System.out.println("Sending " + readyToSendTrackedObjects.size() + " at tick " +  tick.getCounter());
            if (!readyToSendTrackedObjects.isEmpty()) {
                statisticalFolder.addTrackedObjects(readyToSendTrackedObjects.size());
                sendEvent(new TrackedObjectsEvent(readyToSendTrackedObjects));
            }
            if (liDarWorkerTracker.getLastTrackedObjects().isEmpty()&& liDarWorkerTracker.isFinished()) {
                System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Terminating service after final processing.");
                terminateService();
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            MicroService m = terminated.getSender();
            if (m instanceof TimeService) {
                System.out.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Received TerminatedBroadcast from TimeService. Terminating.");
                terminateService();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, crashed -> {
            System.err.println("[LiDAR Worker " + liDarWorkerTracker.getId() + "] Received CrashedBroadcast. Terminating.");
            terminateService();
        });
        latch.countDown();
    }
}
