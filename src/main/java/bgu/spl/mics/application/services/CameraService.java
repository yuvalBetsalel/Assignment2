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
import bgu.spl.mics.application.objects.*;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;
    private Map<Integer, List<DetectedObject>> waitingList;
    protected final StatisticalFolder statisticalFolder;
    protected final ErrorOutput errorOutput;
    private String filePath;
    private CountDownLatch latch;
    /**
     * Constructor for CameraService.
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera, CountDownLatch latch) {
        super(camera.getCamera_key());
        this.camera = camera;
        statisticalFolder = StatisticalFolder.getInstance();
        errorOutput = ErrorOutput.getInstance();
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
        }

        /**
         * Initializes the CameraService.
         * Registers the service to handle TickBroadcasts and sets up callbacks for sending
         * DetectObjectsEvents.
         */
        @Override
        protected void initialize () {
            camera.loadCameraData(filePath);

            subscribeBroadcast(TickBroadcast.class, tick -> {
                int currTick = tick.getCounter();
                for (StampedDetectedObjects stampedObj : camera.getDetectedObjectList()){
                    if (stampedObj.getTime() == currTick){
                        for (DetectedObject obj : stampedObj.getDetectedObjects()){
                            if (obj.getId().equals("ERROR")) {
                                camera.setStatus(STATUS.ERROR);
                                errorOutput.setError(obj.getDescription());
                                errorOutput.setFaultySensor(camera.getCamera_key());
                                statisticalFolder.setSystemRuntime(currTick);
                                sendBroadcast(new CrashedBroadcast(this));
                                terminate();
                            }
                        }
                        waitingList.put(currTick + camera.getFrequency(), stampedObj.getDetectedObjects());
                        break;
                    }
                }
                if (waitingList.containsKey(currTick)){
                    StampedDetectedObjects stampedDetectedObjects = new StampedDetectedObjects(currTick- camera.getFrequency(),
                            waitingList.get(currTick));
                    statisticalFolder.addDetectedObjects(stampedDetectedObjects.getDetectedObjects().size());
                    if (camera.getStatus().equals(STATUS.UP))
                        errorOutput.addCameraFrame(stampedDetectedObjects, camera.getCamera_key());
                    sendEvent(new DetectObjectsEvent(stampedDetectedObjects));
                }
                //check if we already processed all objects
                if (camera.getMaxTime() <= currTick){
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


