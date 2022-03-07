package Sim.Messages;

import Sim.Message;
import Sim.NetworkAddr;

public class IPv6 extends Message {
    public IPv6(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }
}
