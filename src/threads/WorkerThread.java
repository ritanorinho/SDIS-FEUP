package threads;

import java.io.IOException;

import project.Peer;

public class WorkerThread implements Runnable {

	
	String channel;
	String message;
	public WorkerThread(String worker) {
		String[] message = worker.trim().split("-");
		this.channel= message[1];
		this.message= message[0];
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		switch (channel) {
		case "mdb":
			try {
				Peer.getMDBListener().message(this.message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case "mdr":
			try {
				Peer.getMDRListener().message(this.message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "mc":
			try {
				Peer.getMCListener().message(this.message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		}
		
	}

}
