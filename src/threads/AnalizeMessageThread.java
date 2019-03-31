package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;
import utils.Chunk;

public class AnalizeMessageThread implements Runnable {
	String message;
	String[] messageArray;
	
	public AnalizeMessageThread(String msg) {
		this.message = msg;
		this.messageArray = this.message.trim().split("\\s+");
		// TODO Auto-generated constructor stub
	}

	private synchronized void stored() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		if (Peer.getMemory().backupChunks.containsKey(chunkId)) {
			System.out.println("replication: " + Peer.getMemory().backupChunks.get(chunkId));

			Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId) + 1);
		}

	}

	private synchronized void putchunk() {
		
	    String chunkId = messageArray[3] + "-" + messageArray[4];
		 System.out.println("SENDER ID: "+messageArray[2]+" PEER ID: "+Peer.getId());
		Integer id = Integer.parseInt(messageArray[2]);
		Random random = new Random();
		int delay = random.nextInt(401);

		if (!Peer.getMemory().backupChunks.containsKey(chunkId)) {
			Peer.getMemory().backupChunks.put(chunkId, 0);
		} else {
			if (Peer.getId() != id && Peer.getMemory().backupChunks.get(chunkId) < 1) {
				Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId) + 1);
				String storedMessage = "STORED " + messageArray[1] + " " + id + " " + messageArray[3] + " "
						+ messageArray[4] + " " + "\n\r\n\r";

				
				Peer.getExecutor().schedule(new StoredChunkThread(storedMessage.getBytes(),messageArray[6]), delay,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	private synchronized void delete() {

		String fileId = messageArray[3];
		;
		
		if (Peer.getMemory().hasFile(fileId)) {
			Peer.getMemory().removeChunks(fileId);
		}

	}
	private void chunk() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		System.out.println("RECEIVED "+this.message);
		int senderId = Integer.parseInt(messageArray[2]);
		
		if (Peer.getId() != senderId) {
			Peer.getMemory().requiredChunks.put(chunkId, messageArray[5]);		
		}
		
	}

	private void getchunk() {
		String[] messageArray = this.message.trim().split("\\s+");
		String chunkId = messageArray[3] + "-" + messageArray[4];
		
		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);
		if (Peer.getId() != senderId && Peer.getMemory().backupChunks.containsKey(chunkId))
		{
			Peer.getExecutor().schedule(new GetchunkThread(messageArray), delay,
					TimeUnit.MILLISECONDS);
		}

	}

	@Override
	public void run() {
		String messageType = this.message.trim().split("\\s+")[0];
		switch (messageType) {
		case "PUTCHUNK":
			putchunk();
			break;
		case "STORED":
			stored();
			break;
		case "DELETE":
			delete();
			break;
		case "GETCHUNK":
			getchunk();
			break;
		case "CHUNK":
			chunk();
			break;
		case "RESTORE":
			restore();
		default:
		}
	}

	private void restore() {
		int senderId = Integer.parseInt(this.messageArray[1]);
		
		if (Peer.getId()!= senderId)
		Peer.getExecutor().execute(new RestoreFileThread(this.messageArray[2],this.messageArray[3]));
		
	}


}
