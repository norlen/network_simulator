package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router advertisement
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4861#section-4.2
 */
public class RouterAdvertisement extends ICMPv6 {
    // While this message does contain multiple fields, we leave those out. Since we do not support those use-cases.
    
    public RouterAdvertisement(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq, 134, 0);
    }
}
