package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import project.Peer;
import threads.AnalizeMessageThread;


public class MDBListener implements Runnable {

	private Integer mdbPort;
	private InetAddress mdbAddress;

	public MDBListener(InetAddress mdbAddress, Integer mdbPort) {
		this.mdbAddress= mdbAddress;
		this.mdbPort = mdbPort;
	}
	
	
	 @Override
		public void run() {
		 byte[] buf = new byte[65000];
		 MulticastSocket clientSocket;
		try {
			
			clientSocket = new MulticastSocket(this.mdbPort);
			clientSocket.joinGroup(this.mdbAddress);

			 while(true) {
				DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);

				clientSocket.receive(msgPacket);
				
				byte[] message = Arrays.copyOf(buf,msgPacket.getLength());
				Peer.getExecutor().execute(new AnalizeMessageThread(message));
			 }
			 
		} catch (IOException e) {
			e.printStackTrace();
		}     
		
	}
	 
	
	public int message(byte[] message) throws IOException {
		DatagramSocket mcSocket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(message, message.length, mdbAddress, mdbPort);
		mcSocket.send(packet);
		mcSocket.close();
		 return 0;
	 }

}