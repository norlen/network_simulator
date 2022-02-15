package Sim.Traffic;

import Sim.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Traffic sink that processes incoming messages.
 * <p>
 * Can also save results in a CSV file for further processing.
 */
public class TrafficSink extends Node {
    // Stores time between packets and how many packets that have that key.
    private final HashMap<Integer, Integer> _timeBetweenPackets = new HashMap<>();

    // Last received message.
    private Message _lastMessage = null;

    // Last receive time.
    private double _lastRecvTime;

    /**
     * Instantiates a new Traffic Sink.
     *
     * @param network Sink network id.
     * @param node    Sink node id.
     */
    public TrafficSink(int network, int node) {
        super(network, node);
    }

    /**
     * Handle incoming events. Processes incoming messages and stores statistics.
     *
     * @param src SimEnt entity that sent the event.
     * @param ev  Event to be processed.
     */
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof Message) {
            System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " receives message with seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());

            if (_lastMessage != null) {
                double timeDifference = SimEngine.getTime() - _lastRecvTime;
                int millis = (int) timeDifference;

                int packets = _timeBetweenPackets.getOrDefault(millis, 0);
                _timeBetweenPackets.put(millis, packets + 1);
            }
            _lastRecvTime = SimEngine.getTime();
            _lastMessage = (Message) ev;
        }
    }

    /**
     * Writes the processed message stats to `filename`. It will output a csv file containing two columns:
     * time between packets and number of packets.
     *
     * @param filename File that save results in.
     */
    public void saveResults(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("timeBetweenRecv,packets\n");
            for (var entry : _timeBetweenPackets.entrySet()) {
                writer.write(entry.getKey().toString() + "," + entry.getValue().toString() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error when writing message stats.");
        }
    }
}
