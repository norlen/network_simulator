package Sim;

// This class implements an event that send a Message, currently the only
// fields in the message are who the sender is, the destination and a sequence 
// number

public class Message implements Event {
    private NetworkAddr _source;
    private NetworkAddr _destination;
    private int _seq = 0;
    private double _timestamp = 0.0;

    public Message(NetworkAddr from, NetworkAddr to, int seq) {
        _source = from;
        _destination = to;
        _seq = seq;
        _timestamp = SimEngine.getTime();
    }

    public NetworkAddr source() {
        return _source;
    }

    public NetworkAddr destination() {
        return _destination;
    }

    public int seq() {
        return _seq;
    }

    public double getTimestamp() {
        return _timestamp;
    }

    public void entering(SimEnt locale) {
    }
}
	
