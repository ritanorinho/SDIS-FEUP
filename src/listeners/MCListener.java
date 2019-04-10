
package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import project.Peer;
import threads.*;

public class MCListener implements Runnable {
    InetAddress mcAddress;
    Integer mcPort;

    public MCListener(InetAddress mcAddress, Integer mcPort) {
        this.mcAddress = mcAddress;
        this.mcPort = mcPort;

    }

    public MCListener() {}

    @Override
    public void run() {
        byte[] buf = new byte[65000];
        MulticastSocket clientSocket;
        try {

            clientSocket = new MulticastSocket(this.mcPort);
            clientSocket.joinGroup(this.mcAddress);

            while (true) {
                DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                clientSocket.receive(msgPacket);
                byte[] message = Arrays.copyOf(buf, msgPacket.getLength());
                Peer.getExecutor().execute(new AnalizeMessageThread(message, msgPacket.getAddress()));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int message(byte[] message) throws IOException {

        DatagramSocket mcSocket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(message, message.length, mcAddress, mcPort);
        mcSocket.send(packet);
        mcSocket.close();
        return 0;
    }

}