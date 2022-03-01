package Sim;

import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

import java.util.HashMap;

public class HomeAgent extends Node {
    private HashMap<NetworkAddr, NetworkAddr> mappings;

    public HomeAgent(int network, int node, TrafficGenerator generator, Sink sink) {
        super(network, node, generator, sink);
    }

    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof BindingUpdate msg) {
            handleBindingUpdate(msg);
        }
        super.recv(src, ev);
    }

    public void handleBindingUpdate(BindingUpdate msg) {
        // The packet MUST contain a unicast routable home address, either in
        // the Home Address option or in the Source Address, if the Home
        // Address option is not present.

        // The Sequence Number field in the Binding Update is greater than
        // the Sequence Number received in the previous valid Binding Update
        // for this home address, if any.

        // If the receiving node has no Binding Cache entry for the indicated
        // home address, it MUST accept any Sequence Number value in a
        // received Binding Update from this mobile node.

        //   This Sequence Number comparison MUST be performed modulo 2**16,
        //      i.e., the number is a free running counter represented modulo
        //      65536.  A Sequence Number in a received Binding Update is
        //      considered less than or equal to the last received number if its
        //      value lies in the range of the last received number and the
        //      preceding 32768 values, inclusive.  For example, if the last
        //      received sequence number was 15, then messages with sequence
        //      numbers 0 through 15, as well as 32783 through 65535, would be
        //      considered less than or equal.


    }
}
