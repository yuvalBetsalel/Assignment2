package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {

    private int counter;

    public TickBroadcast(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }
}
