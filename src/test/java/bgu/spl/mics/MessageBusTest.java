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
    private CameraService service1, service2, service3;
    private CountDownLatch latch;
    private PoseEvent poseEvent;
    private TickBroadcast tickBroadcast;


    @BeforeEach
    void setup(){
        messageBus = MessageBusImpl.getInstance();
        latch = new CountDownLatch(1);

        service1 = new CameraService(new Camera(1, 1), latch);
        service2 = new CameraService(new Camera(2, 2), latch);
        service3 = new CameraService(new Camera(3, 3), latch);


        messageBus.register(service1);
        messageBus.register(service2);
        messageBus.register(service3);


        poseEvent = new PoseEvent(new Pose(0, 0, 0, 30));
        tickBroadcast = new TickBroadcast(10);
    }

    /**
     * @pre `service1` is registered with the MessageBus.
     * @post `service1` receives the event it subscribed to.
     * @inv The queue size for registered services is non-negative.
     */
    @Test
    void subscribeEvent() {
        messageBus.subscribeEvent(PoseEvent.class, service1);
        Future<Boolean> future = messageBus.sendEvent(poseEvent);
        assertNotNull(future, "Future should not be null when an event is sent.");

        try {
            Message receivedMessage = messageBus.awaitMessage(service1);
            assertEquals(poseEvent, receivedMessage, "Service1 should receive the subscribed event.");
        } catch (InterruptedException e) {
            fail("Unexpected interruption during test.");
        }
    }

    /**
     * @pre `service1` is registered and subscribes to `TickBroadcast`.
     * @post `service1` receives the broadcast.
     * @inv The broadcast message queue size is non-negative.
     */
    @Test
    void subscribeBroadcast() {
        messageBus.subscribeBroadcast(TickBroadcast.class, service1);
        messageBus.sendBroadcast(tickBroadcast);
        try {
            Message receivedMessage = messageBus.awaitMessage(service1);
            assertEquals(tickBroadcast, receivedMessage, "Service1 should receive the broadcast.");
        } catch (InterruptedException e) {
            fail("Unexpected interruption during test.");
        }
    }

    @Test
    void complete() {

    }

    /**
     * @pre Multiple services subscribe to a specific broadcast type.
     * @post All subscribed services receive the broadcast, and unsubscribed services do not.
     * @inv The broadcast message is delivered exactly once to each subscribed service.
     */
    @Test
    void sendBroadcast() {
        // Subscribe multiple services to the broadcast
        messageBus.subscribeBroadcast(CrashedBroadcast.class, service1);
        messageBus.subscribeBroadcast(CrashedBroadcast.class, service2);
        messageBus.subscribeBroadcast(TickBroadcast.class, service3);

        CrashedBroadcast crashedBroadcast = new CrashedBroadcast(service1);
        messageBus.sendBroadcast(crashedBroadcast);
        messageBus.sendBroadcast(new TickBroadcast(1));

        try {
            Message receivedMessage1 = messageBus.awaitMessage(service1);
            Message receivedMessage2 = messageBus.awaitMessage(service2);
            Message receivedMessage3 = messageBus.awaitMessage(service3);
            assertEquals(crashedBroadcast, receivedMessage1, "Service1 should receive crashed broadcast.");
            assertEquals(crashedBroadcast, receivedMessage2, "Service2 should receive crashed broadcast.");
            assertNotEquals(crashedBroadcast, receivedMessage3, "Service1 should not receive crashed broadcast." );
        } catch (InterruptedException e) {
            fail("Unexpected interruption during test.");
        }
    }

    @Test
    void sendEvent() {
//        messageBus.subscribeEvent(PoseEvent.class, service1);
//        Future<Boolean> future = messageBus.sendEvent(poseEvent);
//        assertNotNull(future);
//
//        try {
//            Message receivedMessage = messageBus.awaitMessage(service1);
//            assertEquals(poseEvent, receivedMessage);
//
//            Pose result = new Pose(5, 5, 0, 180);
//            messageBus.complete(poseEvent, result);
//
//            assertTrue(future.isDone());
//            assertEquals(result, future.get());
//        } catch (InterruptedException e) {
//            fail("Unexpected interruption during test.");
//        }
    }


    @Test
    void register() {
        CameraService service2 = new CameraService(new Camera(2, 0), latch); // Example MicroServices
        // check that the new service is not registered
        assertFalse(messageBus.getServiceMap().containsKey(service2));
        // register the new service
        messageBus.register(service2);
        // check again
        assertTrue(messageBus.getServiceMap().containsKey(service2));

        messageBus.register(service3);
        assertTrue(messageBus.getServiceMap().containsKey(service3));
    }

     /*
     * @inv messageBus microservice registration queue size is non-negative
     * @pre messageBus contains service1 as a subscribed MicroService
     * @post messageBus does Not contain service1 as a subscribed MicroService
     */
    @Test
    void unregister() {
        messageBus.register(service1);
        assertTrue(messageBus.getServiceMap().containsKey(service1), "Service1 should be registered before unregistering.");

        messageBus.unregister(service1);
        // Check if the service has been removed from all maps
        assertFalse(messageBus.getServiceMap().containsKey(service1), "Service1 should no longer be registered.");
        assertTrue(messageBus.getEventSubscribers().values().stream().noneMatch(list -> list.contains(service1)),
                "Service1 should not be in event subscribers.");
        assertTrue(messageBus.getBroadcastSubscribers().values().stream().noneMatch(list -> list.contains(service1)),
                "Service1 should not be in broadcast subscribers.");

        //assertFalse(messageBus.getServiceMap().containsKey(service1));

        messageBus.unregister(service3); // Unregistered service
        assertFalse(messageBus.getServiceMap().containsKey(service3));
        // check that the service is registered
        //assertTrue(messageBus.getServiceMap().containsKey(service1));
        messageBus.register(service3);
        messageBus.subscribeEvent(poseEvent.getClass(), service3);
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
        messageBus.subscribeBroadcast(TickBroadcast.class, service1);
        Thread senderThread = new Thread(() -> messageBus.sendBroadcast(tickBroadcast));
        senderThread.start();

        try {
            Message receivedMessage = messageBus.awaitMessage(service1);
            assertEquals(tickBroadcast, receivedMessage);
        } catch (InterruptedException e) {
            fail("Unexpected interruption during test.");
        }
//        //messageBus.register(service3);
//        Thread senderThread = new Thread(() -> {
//            try {
//                Thread.sleep(100); // Simulate delay
//                messageBus.sendBroadcast(broadcast);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
//
//        senderThread.start();
//        try {
//            Message received = messageBus.awaitMessage(service3);
//            assertEquals(broadcast, received);
//        } catch (InterruptedException e) {
//            fail("Unexpected interruption");
//        }
    }
}

