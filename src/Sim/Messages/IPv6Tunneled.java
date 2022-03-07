package Sim.Messages;

import Sim.NetworkAddr;

public class IPv6Tunneled extends IPv6 {
    private final IPv6 _originalPacket;

    public IPv6Tunneled(NetworkAddr from, NetworkAddr to, int seq, IPv6 originalPacket) {
        super(from, to, seq);
        _originalPacket = originalPacket;
    }
}
