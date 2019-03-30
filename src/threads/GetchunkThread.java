package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class GetchunkThread implements Runnable {
	String[] messageArray;

	public GetchunkThread(String[] msg) {
		this.messageArray = msg;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		byte[] chunkData = Peer.getMemory().savedChunks.get(chunkId).getData();
		String chunkContent = new String(chunkData,0,chunkData.length);
	
		String restoredChunk = "CHUNK " + messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
				+ messageArray[4] + "\n\r\n\r"+chunkContent;
		String worker= restoredChunk + "-"+"mdr";
		Random random = new Random();
		int delay = random.nextInt(401);
		
		int senderId = Integer.parseInt(messageArray[2]);
		String filename = "Peer"+Peer.getId() +"/"+"CHUNK"+"/"+messageArray[3] + "/" + messageArray[4];
		try {
			File file = new File(filename);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(chunkContent.getBytes());
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (Peer.getId() != senderId )
		{
			Peer.getExecutor().schedule(new WorkerThread(worker), delay,
					TimeUnit.MILLISECONDS);
		}
		// TODO Auto-generated method stub
	}

}
