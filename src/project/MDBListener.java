package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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
			 
			 DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
			 System.out.println(this.mdbPort+"-"+this.mdbAddress);
	         clientSocket.receive(msgPacket);
	         
	         String msg = new String(buf, 0, buf.length);
	         
	         System.out.println("aaaa"+msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
	}

}
