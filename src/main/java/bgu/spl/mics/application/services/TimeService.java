package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.FusionSlam;

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
            try {
                latch.await();
                while (counter <= duration && FusionSlam.getInstance().isRunning()) {
                    Thread.sleep(tickTime*100); // Sleep for tickTime to simulate real-time ticking
                    sendBroadcast(new TickBroadcast(counter)); // Broadcast the current tick
                    counter++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.messageBus.sendBroadcast(new TerminatedBroadcast(this));
            terminate(); // Signal termination after the duration


    }
}

