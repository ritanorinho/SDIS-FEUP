package threads;

import java.io.IOException;
import project.Peer;

public class DeleteThread implements Runnable {
	private byte[] message;
    
	public DeleteThread(byte[] message) {
		this.message=message;
    }
    
	@Override
	public void run() {
		try {
			Peer.getMCListener().message(message);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
