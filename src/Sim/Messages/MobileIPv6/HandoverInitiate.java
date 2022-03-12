package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Handover Initiate (HI)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.1.1
 */
public class HandoverInitiate extends MobilityHeader {
    // Identifier so Handover Acknowledgements can be matched.
    private final int _identifier;

    /**
     * Message sent by the current access router to the next access router to initiate a handover.
     *
     * @param from address of the current access router.
     * @param to   address of th next access router.
     * @param seq  sequence number.
     */
    public HandoverInitiate(NetworkAddr from, NetworkAddr to, int seq, int identifier) {
        super(from, to, seq);
        _identifier = identifier;
    }

    public int getIdentifier() {
        return _identifier;
    }
}
