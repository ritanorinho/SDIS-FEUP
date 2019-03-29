package listeners;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import project.Peer;
import threads.AnalizeMessageThread;


public class MDBListener implements Runnable {

	private Integer mdbPort;
	private InetAddress mdbAddress;
	public MDBListener(InetAddress mdbAddress, Integer mdbPort) {
			this.mdbAddress= mdbAddress;
			this.mdbPort = mdbPort;
		// TODO Auto-generated constructor stub
	}
	 @Override
		public void run() {
		 byte[] buf = new byte[256];
		 MulticastSocket clientSocket;
		try {
			
			clientSocket = new MulticastSocket(this.mdbPort);
			 clientSocket.joinGroup(this.mdbAddress);
			 while(true) {
			 DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
			 //System.out.println(this.mdbPort+"-"+this.mdbAddress);
	         clientSocket.receive(msgPacket);
	         
	         String msg = new String(buf, 0, buf.length);
	         System.out.println(msg);
	         
	         Peer.getExecutor().execute(new AnalizeMessageThread(msg));
			 }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
	}
	 
	
	public int message(byte[] message) throws IOException {
		MulticastSocket mcSocket = new MulticastSocket();
		DatagramPacket packet = new DatagramPacket(message, message.length, mdbAddress, mdbPort);
		mcSocket.send(packet);
		mcSocket.close();
		 return 0;
	 }

}