package threads;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class BackupThread implements Runnable {
	private byte[] message;
	private int replicationDegree;
	private String hashName;
	private int attempt = 0;
	private final int MAX_ATTEMPT = 5;
	private int delay;

	public BackupThread(String hashName, byte[] message, int repDegree) {

		this.hashName = hashName;
		this.message = message;
		this.delay = 1;
		this.replicationDegree = repDegree;

	}

	@Override
	public void run() {
		int replicationState = Peer.getMemory().savedOcurrences.get(this.hashName);

		// if the number of confirmation messages it received up to the end
		// of that interval is lower than the desired replication degree
		int delta = this.replicationDegree - replicationState;
		this.delay = this.delay * 2;
		if (delta > 0) {

			// it retransmits the backup message on the MDB channel, and
			// doubles the time interval for receiving confirmation messages
			try {
				Peer.getMDBListener().message(this.message);
			} catch (IOException e) {
				e.printStackTrace();
			}

			this.attempt++;
			System.out.println("delay " + this.delay + " attempt " + this.attempt);

			if (this.attempt < this.MAX_ATTEMPT) { // This procedure is repeated up to a maximum number of five times
				Peer.getExecutor().schedule(this, this.delay, TimeUnit.SECONDS);
			}

			else {
				System.out.println("PUTCHUNK THREAD: reached the maximum number of retransmissions per chunk");
			}
		} else {
			String noChunk= hashName.split("-")[1];
			System.out.println("CHUNK NO " + noChunk+ ": REPLICATION DEGREE ACHIEVED: " + replicationState + "\n");
		}
	}

}
