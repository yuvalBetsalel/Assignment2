package bgu.spl.mics.application.services;

import bgu.spl.mics.Event;
import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("camera" + camera.getId());
        this.camera = camera;
        statisticalFolder = StatisticalFolder.getInstance();
        waitingList = new ConcurrentHashMap<>();
        filePath = "";
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private void loadCameraData() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type cameraDataType = new TypeToken<Map<String, List<StampedDetectedObjects>>>() {}.getType();
            Map<String, List<StampedDetectedObjects>> cameraData = gson.fromJson(reader, cameraDataType);
            for (Map.Entry<String, List<StampedDetectedObjects>> entry : cameraData.entrySet()){
                String cameraId = entry.getKey();
                if (cameraId.equals("camera"+camera.getId())){
                    List<StampedDetectedObjects> stampedObjects = entry.getValue();
                    for (StampedDetectedObjects stampedObj : stampedObjects) {
                        camera.addStampedObject(stampedObj);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            loadCameraData();
            subscribeBroadcast(TickBroadcast.class, tick -> {
                int currTick = tick.getCounter();
                for (StampedDetectedObjects stampedObj : camera.getDetectedObjectList()){
                    if (stampedObj.getTime() == currTick){
                        for (DetectedObject obj : stampedObj.getDetectedObjects()){
                            if (obj.getId().equals("ERROR")) {
                                camera.setStatus(STATUS.ERROR);
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
                    //Future<StampedDetectedObjects> future = ?
                    this.messageBus.sendEvent(new DetectObjectsEvent(stampedDetectedObjects));
                    statisticalFolder.addDetectedObjects();
                }
                if (camera.getDetectedObjectList().getLast().getTime() + camera.getFrequency() < currTick){
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
        }
}


