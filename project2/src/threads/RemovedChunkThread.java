package threads;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import app.Peer;

public class RemovedChunkThread implements Runnable {
	String chunkId ;
	private String fileId;
	private String chunkNo;
	
	public RemovedChunkThread(String chunkId) {
		this.chunkId=chunkId;
		this.fileId = this.chunkId.trim().split("-")[0];
		this.chunkNo = this.chunkId.trim().split("-")[1];
	}

	@Override
	public void run() {
		int replicationDegree;
		byte[] body=new byte[64000];
		byte[] content= new byte[64000];
		int size=0;
		
		
		if (Peer.getMemory().savedChunks.containsKey(this.chunkId)) {
			replicationDegree = Peer.getMemory().savedChunks.get(this.chunkId).getReplicationDegree();
			if (Peer.getMemory().savedOcurrences.get(this.chunkId) < replicationDegree) {
			
				String filePath = "Peer"+Peer.getId()+"/"+"STORED"+"/"+this.fileId+"/"+this.chunkNo+"-"+replicationDegree;
				File file = new File(filePath);
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis);
					while((size=bis.read(content))>0) {
						body = Arrays.copyOf(content, size);
						content = new byte[64000];
					}
					bis.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}

				String header ="PUTCHUNK " +Peer.getProtocolVersion()+" "+Peer.getId() + " " + this.fileId + " "
						+this.chunkNo + " " + replicationDegree + " " + "\r\n\r\n";
				System.out.println("\nREMOVED "+header);
				byte[] message = new byte[header.getBytes().length+body.length];
				System.arraycopy(header.getBytes(), 0, message, 0, header.getBytes().length);
				System.arraycopy(body, 0, message, header.getBytes().length, body.length);
			
				// The initiator-peer collects the confirmation
				// messages during a time interval of one second
				}
			else {
				System.out.println("The count doesn't drop below the desired replication degree of the chunk "+this.chunkNo);
			}
		}
	}
}
