
package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class MCListener implements Runnable {
		InetAddress mcAddress;
		Integer mcPort;

	 public MCListener(InetAddress mcAddress, Integer mcPort) {
			this.mcAddress=mcAddress;
			this.mcPort=mcPort;
			
	}
	 public MCListener() {
		// TODO Auto-generated constructor stub
	}

	 @Override
		public void run() {
		 byte[] buf = new byte[256];
		 MulticastSocket clientSocket;
		try {
			
			clientSocket = new MulticastSocket(this.mcPort);
			 clientSocket.joinGroup(this.mcAddress);
			 
			 DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
			 System.out.println(this.mcPort+"-"+this.mcAddress);
	         clientSocket.receive(msgPacket);
	         
	         String msg = new String(buf, 0, buf.length);
	         
	         System.out.println("msg: "+msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
	}
	
}