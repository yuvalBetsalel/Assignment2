package bgu.spl.mics;

import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.services.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageBusTest {
    private MessageBusImpl messageBus;
    private MicroService mockService;
    private Event<String> mockEvent;
    private Broadcast mockBroadcast;
//    private MicroService microService;
//    private PoseEvent event;
//    private TickBroadcast broadcast;

    @BeforeEach
    void setup(){
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

    @Test
    void subscribeBroadcast() {
        messageBus.register(microService);
        messageBus.subscribeBroadcast((Class<? extends Broadcast>) broadcast.getClass(), microService);
        messageBus.sendBroadcast(broadcast);
        try{
            Message received = messageBus.awaitMessage(microService);
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

    @Test
    void register() {

    }

    @Test
    void unregister() {
        messageBus.register(microService);
        messageBus.subscribeEvent((Class<? extends Event<Pose>>) event.getClass(), microService);
        messageBus.unregister(microService);
        Future<String> future = messageBus.sendEvent(event);
        assertNull(future); // Event should not be routed
    }

    @Test
    void awaitMessage() {
        messageBus.register(microService);
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
            Message received = messageBus.awaitMessage(microService);
            assertEquals(broadcast, received);
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }
    }
}