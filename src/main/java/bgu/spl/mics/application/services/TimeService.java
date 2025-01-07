package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.FusionSlam;

import java.util.concurrent.CountDownLatch;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class    TimeService extends MicroService {
    private CountDownLatch latch;
    private int tickTime;
    private int duration;
    private int counter;
    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration, CountDownLatch latch) {
        super("TimeService");  //what is name??
        tickTime = TickTime;
        duration = Duration;
        counter = 1;
        this.latch = latch;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
        //if (messageBus.getInitializeCounter() == 0) {
            try {
                latch.await();
                System.out.println("all services are initialized");
                while (counter < duration && FusionSlam.getInstance().isRunning()) {
                    sendBroadcast(new TickBroadcast(counter)); // Broadcast the current tick
                    System.out.println("sent tick " + counter);
                    counter++;
                    Thread.sleep(tickTime); // Sleep for tickTime to simulate real-time ticking
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
            System.out.println("time service is done");
            terminate(); // Signal termination after the duration
    }

}

