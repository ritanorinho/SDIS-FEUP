package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MDRListener implements Runnable{

	private InetAddress mdrAddress;
	private Integer mdrPort;

	public MDRListener(InetAddress mdrAddress, Integer mdrPort) {
			this.mdrAddress = mdrAddress;
			this.mdrPort = mdrPort;
	}

	public void run() {
		 byte[] buf = new byte[256];
		 MulticastSocket clientSocket;
		try {
			
			clientSocket = new MulticastSocket(this.mdrPort);
			 clientSocket.joinGroup(this.mdrAddress);
			 
			 DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
			 System.out.println(this.mdrPort+"-"+this.mdrAddress);
	         clientSocket.receive(msgPacket);
	         
	         String msg = new String(buf, 0, buf.length);
	         
	         System.out.println("msg: "+msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
	}

}