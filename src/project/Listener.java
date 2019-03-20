package project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Listener implements Runnable {
	public MulticastSocket socket;

	public static InetAddress address;
	public static int port;
	public Listener(InetAddress address, int port) {
		
		this.address=address;
		this.port=port;
	}
	

	public Listener() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
	
	}
	
	

}
