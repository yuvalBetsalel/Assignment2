package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {

    //private String senderId;
    private int counter;

    public TickBroadcast(int counter) {
   //     this.senderId = senderId;
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }
//    public String getSenderId() {
//        return senderId;
//    }
}
