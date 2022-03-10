package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Handover Acknowledge (HAck)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.1.2
 */
public class HandoverAcknowledge extends MobilityHeader {

    /**
     * Message sent as a reply to the Handover Initiate (HI) message.
     *
     * @param from the HI destination address.
     * @param to   the HI source address.
     * @param seq  sequence number.
     */
    public HandoverAcknowledge(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq, 0);
    }
}
