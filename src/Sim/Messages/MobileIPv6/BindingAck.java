package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Binding Acknowledgement
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1.8
 */
public class BindingAck extends MobilityHeader {
    public static final int STATUS_ACCEPTED = 0;
    public static final int STATUS_ACCEPTED_BUT_DISCOVERY_NECESSARY = 1;
    public static final int REJECTED_REASON_UNSPECIFIED = 128;
    public static final int REJECTED_ADMINISTRATIVELY_PROHIBITED = 129;
    // ...
    public static final int REJECTED_INVALID_CARE_OF_ADDRESS = 174;

    // Status code of the binding, should be one of the defined values in the class. Values greater or equal to 128
    // means the update was rejected.
    private final int _status = 0;

    // Copied from incoming Binding Update so clients can match with correct one.
    private final int _sequence = 0;

    public BindingAck(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }

    public int getSequence() {
        return _sequence;
    }
}
