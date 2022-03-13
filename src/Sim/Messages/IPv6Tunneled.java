package Sim.Messages;

import Sim.Message;
import Sim.NetworkAddr;

public class IPv6Tunneled extends IPv6 {
    private final Message _originalPacket;

    public IPv6Tunneled(NetworkAddr from, NetworkAddr to, int seq, Message originalPacket) {
        super(from, to, seq);
        _originalPacket = originalPacket;
    }

    public Message getOriginalPacket() {
        return _originalPacket;
    }

    @Override
    public String toString() {
        return String.format("IPv6Tunneled, src=%s, dst=%s", source(), destination());
    }
}
