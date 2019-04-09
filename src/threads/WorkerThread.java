package threads;

import java.io.IOException;

import project.Peer;

public class WorkerThread implements Runnable {

	
	String channel;
	byte[] message;


	public WorkerThread(byte[] message, String channel) {
		this.message=message;
		this.channel=channel;
		
		// TODO Auto-generated constructor stub
	}
	
	

	@Override
	public void run() {
		
		switch (channel) {
		case "mdb":
			try {
				Peer.getMDBListener().message(this.message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
			
		case "mdr":
			try {
				Peer.getMDRListener().message(this.message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case "mc":
			try {
				
				Peer.getMCListener().message(this.message);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		default:
						
		}
		
	}

}
