package bgu.spl.mics.application.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;
    //private Event<? extends Object> DetectObjectsEvent;
    private Map<Integer, List<DetectedObject>> waitingList;
    protected final StatisticalFolder statisticalFolder;
    private String filePath;
    private CountDownLatch latch;
    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera, CountDownLatch latch) {
        super(camera.getCamera_key());
        this.camera = camera;
        statisticalFolder = StatisticalFolder.getInstance();
        waitingList = new ConcurrentHashMap<>();
        filePath = "";
        this.latch = latch;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

        private void terminateService(){
            camera.setStatus(STATUS.DOWN);
            this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
            this.terminate(); //microService function
            //this.messageBus.unregister(this);
        }

        /**
         * Initializes the CameraService.
         * Registers the service to handle TickBroadcasts and sets up callbacks for sending
         * DetectObjectsEvents.
         */
        @Override
        protected void initialize () {
            camera.loadCameraData(filePath);
            //System.out.println("[Camera Service] Loaded camera data from file: " + filePath);

            subscribeBroadcast(TickBroadcast.class, tick -> {
                int currTick = tick.getCounter();
                System.out.println("[Camera " + camera.getCamera_key() + "] Tick: " + currTick);
                for (StampedDetectedObjects stampedObj : camera.getDetectedObjectList()){
                    if (stampedObj.getTime() == currTick){
                        for (DetectedObject obj : stampedObj.getDetectedObjects()){
                            if (obj.getId().equals("ERROR")) {
                                System.err.println("[Camera " + camera.getCamera_key() + "] ERROR detected: "
                                        + obj.getDescription() + " at tick: " + currTick);
                                camera.setStatus(STATUS.ERROR);
                                sendBroadcast(new CrashedBroadcast(this));
                                terminate();
                            }
                        }
                        System.out.println("[Camera " + camera.getCamera_key() + "] Detected objects at time: " + currTick);
                        waitingList.put(currTick + camera.getFrequency(), stampedObj.getDetectedObjects());
                        break;
                    }
                }
                if (waitingList.containsKey(currTick)){
                    StampedDetectedObjects stampedDetectedObjects = new StampedDetectedObjects(currTick- camera.getFrequency(),
                            waitingList.get(currTick));
                    System.out.println("[Camera " + camera.getCamera_key() + "] Sending detected objects of time: "
                            + stampedDetectedObjects.getTime() + " at tick: " + currTick);
                    sendEvent(new DetectObjectsEvent(stampedDetectedObjects));
                    statisticalFolder.addDetectedObjects(stampedDetectedObjects.getDetectedObjects().size());
                }
                //check if we already processed all objects
                int lastIndex = camera.getDetectedObjectList().size()-1;
                if (camera.getDetectedObjectList().get(lastIndex).getTime() + camera.getFrequency() < currTick){
                    System.out.println("[Camera " + camera.getCamera_key() + "] All objects processed. Terminating service.");
                    terminateService();
                }
            });

            subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
                MicroService m = terminated.getSender();
                if (m instanceof TimeService) {
                    System.out.println("[Camera " + camera.getCamera_key() + "] Received TerminatedBroadcast from TimeService. Terminating.");
                    terminateService();
                }
            });

            subscribeBroadcast(CrashedBroadcast.class, crashed -> {
                System.err.println("[Camera " + camera.getCamera_key() + "] Received CrashedBroadcast. Terminating.");
                terminateService();
            });
            latch.countDown();
        }
}


