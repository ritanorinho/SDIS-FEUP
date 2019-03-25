package threads;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class BackupThread implements Runnable {
	private byte[] message;
	private int delay=1;
	private int replicationDegree;
	private String hashName;
	private int attempt=1;
	private final int MAX_ATTEMPT =5;
	public BackupThread(String hashName,byte[] message, int repDegree) {
		this.hashName=hashName;
		this.message=message;
		this.replicationDegree=repDegree;
		// TODO Auto-generated constructor stub
	}
	@Override
	public void run() {
		int replicationState = Peer.getMemory().backupChunks.get(this.hashName);
		
		if (replicationState <= this.replicationDegree) { // the number of confirmation messages it received up to the end of that interval is lower than the desired replication degree
			this.delay*= 2;
			try {
				Peer.getMDBListener().message(this.message); //it retransmits the backup message on the MDB channel, and doubles the time interval for receiving confirmation messages.
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("PUTCHUNK attempt: "+this.attempt);
			
			if (this.attempt < this.MAX_ATTEMPT) { // This procedure is repeated up to a maximum number of five times
				Peer.getExecutor().schedule(this,this.attempt,TimeUnit.SECONDS);
			}
			else {
				System.out.println("PUTCHUNK THREAD: reached the maximum number of retransmissions per chunk");
			}
			this.attempt++;
			System.out.println("REPLICATION: "+replicationState);
			
		}
		else {
			System.out.println("REPLICATION DEGREE REACHED!");
		}
		
		
		
		
	}

}
