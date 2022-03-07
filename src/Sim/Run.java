package Sim;

// An example of how to build a topology and starting the simulation engine

import Sim.Mobility.HomeAgent;
import Sim.Traffic.*;

import java.util.Date;

enum TrafficGeneratorType {
    NONE,
    CBR,
    GAUSSIAN,
    POISSON,
}

public class Run {
    public static void main(String[] args) {
        boolean useLossyLinks = false;
        TrafficGeneratorType trafficGeneratorType = TrafficGeneratorType.CBR;
        int packetsToSend = 10;
        int lossDelay = 100;
        int lossJitter = 5;
        double lossDrop = 0.05;

        // Creates two links.
        Link link1, link2;
        if (useLossyLinks) {
            // Use lossy links.
            link1 = new LossyLink(lossDelay, lossJitter, lossDrop);
            link2 = new LossyLink(lossDelay, lossJitter, lossDrop);
        } else {
            // Use regular links.
            link1 = new Link();
            link2 = new Link();
        }

        TrafficGenerator host1_traffic = null;
        TrafficGenerator host2_traffic = new ConstantBitRate(packetsToSend, 100);
        Sink host1_sink = null;
        Sink host2_sink = null;

        // Create two end hosts that will be communicating via the router
        if (trafficGeneratorType == TrafficGeneratorType.CBR) {
            host1_traffic = new ConstantBitRate(packetsToSend, 100);
            host2_sink = new FileSink();
        } else if (trafficGeneratorType == TrafficGeneratorType.GAUSSIAN) {
            host1_traffic = new Gaussian(packetsToSend, 100, 25);
            host2_sink = new FileSink();
        } else {
            host1_traffic = new Poisson(packetsToSend, 100);
            host2_sink = new FileSink();
        }

        NetworkAddr host1Addr = new NetworkAddr(1, 1);
        NetworkAddr host2Addr = new NetworkAddr(2, 1);
        Node host1 = new Node(host1Addr.networkId(), host1Addr.nodeId(), host1_traffic, host1_sink);
        Node host2 = new Node(host2Addr.networkId(), host2Addr.nodeId(), host2_traffic, host2_sink);

        // Connect links to hosts
        host1.setPeer(link1);
        host2.setPeer(link2);

        // Creates as router and connect links to it. Information about the host connected to the other side of the link
        // is also provided.
        //
        // Note. A switch is created in same way using the Switch class
        Router routeNode = new HomeAgent(5);
        routeNode.connectInterface(0, 1, link1);
        routeNode.connectInterface(1, 2, link2);

        // temp
        //host1.setRouter(routeNode);
        //host2.setRouter(routeNode);

        // Generate some traffic
        host1.StartSending(host2Addr, 0);
        host2.StartSending(host1Addr, 0);

        // Start the simulation engine and of we go!
        Thread t = new Thread(SimEngine.instance());
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            System.out.println("The motor seems to have a problem, time for service?");
        }

        if (false && trafficGeneratorType != TrafficGeneratorType.NONE) {
            // Output results from sink.
            var h2 = (FileSink) host2.getSink();
            String filename = trafficGeneratorType.toString();
            if (useLossyLinks) {
                filename += "_lossy_d" + lossDelay + "_j" + lossJitter + "_p" + lossDrop;
                int numDropped = ((LossyLink) link1).getNumDroppedPackets() + ((LossyLink) link2).getNumDroppedPackets();
                filename += "_dropped_" + numDropped;
            }
            String suffix = "";
            while (packetsToSend % 1000 == 0) {
                if (suffix.equals("")) {
                    suffix = "k";
                } else if (suffix.equals("k")) {
                    suffix = "m";
                } else {
                    break;
                }
                packetsToSend /= 1000;
            }
            filename += "_pkts_" + packetsToSend + suffix;
            filename += "_" + (new Date()).getTime();
            filename += ".csv";
            h2.saveTimeBetweenPackages(filename);
        }
    }
}
