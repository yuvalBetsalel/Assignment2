package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;

public class CrashedBroadcast implements Broadcast {
    private MicroService sender;

    public CrashedBroadcast(MicroService sender){
        this.sender = sender;
    }

    public MicroService getSender() {
        return sender;
    }
}
