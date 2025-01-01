package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;

public class TerminatedBroadcast implements Broadcast {
    private MicroService sender;

    public TerminatedBroadcast(MicroService sender){
        this.sender = sender;
    }

    public MicroService getSender() {
        return sender;
    }
}
