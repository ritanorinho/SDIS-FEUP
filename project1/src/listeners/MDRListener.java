package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import project.Peer;
import threads.AnalizeMessageThread;
import utils.Utils;

public class MDRListener implements Runnable {

	private InetAddress mdrAddress;
	private Integer mdrPort;

	public MDRListener(InetAddress mdrAddress, Integer mdrPort) {
		this.mdrAddress = mdrAddress;
		this.mdrPort = mdrPort;
	}

	public void run() {
		byte[] buf = new byte[65000];
		MulticastSocket clientSocket;
		try {

			clientSocket = new MulticastSocket(this.mdrPort);
			clientSocket.joinGroup(this.mdrAddress);
			while (true) {
				DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);

				clientSocket.receive(msgPacket);
				byte[] message = Arrays.copyOf(buf, msgPacket.getLength());

				if (validMessage(message))
					Peer.getExecutor().execute(new AnalizeMessageThread(message, msgPacket.getAddress()));
				else
					System.out.println("Ignoring message...");

				Peer.getExecutor().execute(new AnalizeMessageThread(message, msgPacket.getAddress()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int message(byte[] message) throws IOException {
		DatagramSocket mcSocket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(message, message.length, mdrAddress, mdrPort);
		mcSocket.send(packet);
		mcSocket.close();
		return 0;
	}

	public boolean validMessage(byte[] message) {
		String type = Utils.byteArrayToStringArray(message)[0];

		if (type.equals("GETCHUNK"))
			return true;
		else
			return false;
	}

}