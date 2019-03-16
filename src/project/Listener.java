package project;

import java.net.InetAddress;
import java.net.MulticastSocket;

public class Listener implements Runnable {
	public MulticastSocket socket;

	public InetAddress address;
	public int port;
	public Listener(InetAddress address, int port) {
		this.address=address;
		this.port=port;
	}
	

	public Listener() {
		// TODO Auto-generated constructor stub
	}


	public void produce() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	public void consume() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	

}
