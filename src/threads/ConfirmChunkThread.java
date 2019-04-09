package threads;

import java.io.IOException;
import project.Peer;

public class ConfirmChunkThread implements Runnable {
    private byte[] message;

    public ConfirmChunkThread(byte[] message) {
		this.message=message;
    }
    
	@Override
	public void run() {
		try {
			Peer.getMDRListener().message(message);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
