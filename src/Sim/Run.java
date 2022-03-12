package Sim;

// An example of how to build a topology and starting the simulation engine

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

        var HNAddress = new NetworkAddr(0x1111_0000_0000_0000L, 0, 32);
        var FNAddress = new NetworkAddr(0x2222_0000_0000_0000L, 0, 32);
        var MNAddress = new NetworkAddr(0x1111_0000_0000_0000L, 1);
        var CNAddress = new NetworkAddr(0x2222_0000_0000_0000L, 2);

        Node host1 = new Node("MN", MNAddress, HNAddress, host1_traffic, host1_sink);
        Node host2 = new Node("CN", CNAddress, FNAddress, host2_traffic, host2_sink);

        // Connect links to hosts
        host1.setPeer(link1);
        host2.setPeer(link2);

        // Creates as router and connect links to it. Information about the host connected to the other side of the link
        // is also provided.
        //
        // Note. A switch is created in same way using the Switch class

        Router routeNode = new Router("HA", 5, HNAddress);
        Router routeNode2 = new Router("R2", 5, FNAddress);
        routeNode.connectInterface(0, MNAddress, link1);
        routeNode2.connectInterface(0, CNAddress, link2);

        Link routerToRouter = new Link();
        routeNode.connectInterface(1, FNAddress, routerToRouter);
        routeNode2.connectInterface(1, HNAddress, routerToRouter);

        //SimEngine.instance().register(link1, routeNode, new EnterNetwork(host1, 0), 0);
        //SimEngine.instance().register(link2, routeNode2, new EnterNetwork(host2, 0), 0);

        // Generate some traffic
        host1.StartSending(CNAddress, 0);
        host2.StartSending(MNAddress, 0);

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
