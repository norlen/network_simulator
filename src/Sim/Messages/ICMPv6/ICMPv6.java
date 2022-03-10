package Sim.Messages.ICMPv6;

import Sim.Messages.IPv6;
import Sim.NetworkAddr;

/**
 * Implementation of ICMPv6 messages.
 * <p>
 * This class should be subclassed for each specific ICMPv6 message. Note that the class leaves out the checksum
 * calculation. This is mostly for ease of implementation since we don't actually put the message into a byte array.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4443
 */
public abstract class ICMPv6 extends IPv6 {
    // This class makes the simplification of using fields for the required values, instead of packing the values into
    // 32-bits. In part to make it easier to use, as well as to not having to mess with conversion between host byte
    // order, and network byte order.

    // The ICMPv6 message type.
    private final int _type;

    // The ICMPv6 message code.
    private final int _code;

    /**
     * Creates a new ICMPv6 message.
     *
     * @param from source address.
     * @param to   destination address.
     * @param seq  sequence number.
     * @param type ICMPv6 message type.
     * @param code ICMPv6 message code.
     */
    public ICMPv6(NetworkAddr from, NetworkAddr to, int seq, int type, int code) {
        super(from, to, seq);
        _type = type;
        _code = code;
    }

    /**
     * @return ICMPv6 message type.
     */
    public int getType() {
        return _type;
    }

    /**
     * @return ICMPv6 message code.
     */
    public int getCode() {
        return _code;
    }
}
