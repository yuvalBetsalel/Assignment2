package bgu.spl.mics;

import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.services.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.objects.Pose;


class MessageBusTest {
    private MessageBusImpl messageBus;
    private CameraService service1;
    private CountDownLatch latch;
    private MicroService service3;
    private PoseEvent event;
    private TickBroadcast broadcast;

    @BeforeEach
    void setup(){
        messageBus = MessageBusImpl.getInstance();
        //List<StampedDetectedObjects> l1 = new ArrayList<>();
        latch = new CountDownLatch(1);
        service1 = new CameraService(new Camera(1,1),latch);
        service3 = new CameraService(new Camera(3,6),latch);
        messageBus.register(service1);
        event = new PoseEvent(new Pose(0,0,0,30));
        broadcast = new TickBroadcast(10);
//        messageBus = MessageBusImpl.getInstance();
//        mockService = mock(MicroService.class);
//        mockEvent = mock(Event.class);
//        mockBroadcast = mock(Broadcast.class);


//        microService = new TimeService(1,10);
//        event = new PoseEvent(new Pose(3,2,1,40));
//        broadcast = new TickBroadcast(4);
    }


    @Test
    void subscribeEvent() {

//        messageBus.register(microService);
//        messageBus.subscribeEvent((Class<? extends Event<Pose>>) event.getClass(),microService);
//        Future<Pose> future = messageBus.sendEvent(event);
//        assertNotNull(future);
    }

     /*
     *@inv subscribers queue size is non-negative
     * @pre no service is subscribed to get broadcasts
     * @post a new service camService is subscribed to get broadcasts
     */
    @Test
    void subscribeBroadcast() {
        assertTrue(messageBus.getServiceMap().isEmpty());
        MicroService cameraService = new CameraService(new Camera(4, 1), latch);
        messageBus.subscribeBroadcast(CrashedBroadcast.class, cameraService);
        CopyOnWriteArrayList<MicroService> subscribers = messageBus.getBroadcastSubscribers().get(CrashedBroadcast.class);
        assertTrue(subscribers.contains(cameraService));

        messageBus.register(service3);
        messageBus.subscribeBroadcast(broadcast.getClass(), service3);
        messageBus.sendBroadcast(broadcast);

        try{
            Message received = messageBus.awaitMessage(service3);
            assertEquals(broadcast, received);
        } catch (InterruptedException e){
            fail("Unexpected interruption");
        }
    }

    @Test
    void complete() {
    }

    @Test
    void sendBroadcast() {
    }

    @Test
    void sendEvent() {
    }

     /*
     * @inv messageBus microservice registration queue size is non-negative
     * @pre messageBus does not contain any subscribed MicroServices other than service 1
     * @post messageBus contains service2 as a subscribed MicroServices
     */
    @Test
    void register() {
        CameraService service2 = new CameraService(new Camera(2, 0), latch); // Example MicroServices
        // check that the new service is not registered
        assertFalse(messageBus.getServiceMap().containsKey(service2));
        // register the new service
        messageBus.register(service2);
        // check again
        assertTrue(messageBus.getServiceMap().containsKey(service2));
    }

     /*
     * @inv messageBus microservice registration queue size is non-negative
     * @pre messageBus contains service1 as a subscribed MicroService
     * @post messageBus does Not contain service1 as a subscribed MicroService
     */
    @Test
    void unregister() {
        // check that the service is registered
        assertTrue(messageBus.getServiceMap().containsKey(service1));
        messageBus.register(service3);
        messageBus.subscribeEvent((Class<? extends Event<Pose>>) event.getClass(), service3);
        assertTrue(messageBus.getServiceMap().containsKey(service3));
        messageBus.unregister(service3);
        messageBus.unregister(service1);
//        Future<String> future = messageBus.sendEvent(event);
//        assertNull(future); // Event should not be routed
        // check that the services is unregistered
        assertFalse(messageBus.getServiceMap().containsKey(service3));
        assertFalse(messageBus.getServiceMap().containsKey(service1));
    }

    @Test
    void awaitMessage() {
        messageBus.register(service3);
        Thread senderThread = new Thread(() -> {
            try {
                Thread.sleep(100); // Simulate delay
                messageBus.sendBroadcast(broadcast);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        senderThread.start();
        try {
            Message received = messageBus.awaitMessage(service3);
            assertEquals(broadcast, received);
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }
}

