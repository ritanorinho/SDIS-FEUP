package threads;

import java.io.IOException;

import project.Peer;

public class StoredChunkThread implements Runnable {
	byte[] message;
	
	public StoredChunkThread(byte[] storedMessage) {
		this.message=storedMessage;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			Peer.getMCListener().message(message);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
