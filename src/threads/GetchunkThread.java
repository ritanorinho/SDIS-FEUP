package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class GetchunkThread implements Runnable {
	private String[] messageArray;
	private String chunkId;

	public GetchunkThread(String[] msg) {
		this.messageArray = msg;
		this.chunkId = messageArray[3] + "-" + messageArray[4];
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		if (!Peer.getMemory().savedChunks.containsKey(chunkId)) {
			System.out.println("This peer doesn't contain this chunk: "+chunkId);
		}
		else {
		byte[] chunkData = Peer.getMemory().savedChunks.get(chunkId).getData();
	
		String restoredChunk = "CHUNK " + messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
				+ messageArray[4] + "\r\n\r\n";
		
		byte[] data = restoredChunk.getBytes();
		byte[] message = new byte[data.length + chunkData.length];
		System.arraycopy(data, 0, message, 0, data.length);
		System.arraycopy(chunkData, 0, message, data.length, chunkData.length);
		String channel ="mdr";
		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);
		if (Peer.getId() != senderId )
		{	
			Peer.getExecutor().schedule(new WorkerThread(message,channel), delay,
					TimeUnit.MILLISECONDS);
		}
		// TODO Auto-generated method stub
	}
	}
	

}
