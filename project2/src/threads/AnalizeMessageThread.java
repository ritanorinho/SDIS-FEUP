package threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import app.Peer;
import threads.scheduled.SaveMemoryTask;
import utils.Chunk;
import utils.FileInfo;
import utils.Utils;

public class AnalizeMessageThread implements Runnable {
	String message;
	String[] messageArray;
	byte[] messageBytes;
	String chunkId;
	InetAddress InetAddress;
	int senderId;
	Socket socket;

	public AnalizeMessageThread(byte[] message, Socket socket) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = Utils.byteArrayToStringArray(message);
		this.socket = socket;
		if (messageArray.length > 4)
			this.chunkId = this.messageArray[2] + "-" + this.messageArray[3];
		else if (messageArray.length > 3)
			this.chunkId = this.messageArray[2];
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
		case "RESTOREFILE":
			restoreFile();
			break;
		default:
		}
	}


	private synchronized void stored() {
		String chunkId = messageArray[2] + "-" + messageArray[3];
		if (Peer.getMemory().savedOcurrences.containsKey(chunkId) && Peer.getId() != senderId) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) + 1);
		}

		try {
			Peer.sendMessageToServer("STORED " +  messageArray[1] + " "+chunkId+" "+messageArray[4]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void putchunk() {
		Integer id = Integer.parseInt(messageArray[1]);
		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != id && !Peer.getMemory().savedChunks.containsKey(chunkId)) {
			if (!Peer.getMemory().savedOcurrences.containsKey(chunkId)) {
				Peer.getMemory().savedOcurrences.put(chunkId, 0);
			}
			String storedMessage =messageArray[1] + " " + messageArray[2] + " "
					+ messageArray[3];
					System.out.println(storedMessage);
			byte[] data = Utils.getBody(this.messageBytes);
			Peer.getExecutor().schedule(
					new StoredChunkThread(storedMessage.getBytes(), data, Integer.parseInt(messageArray[4]),socket), delay,
					TimeUnit.MILLISECONDS);
		}
	}

	private void getchunk() {

		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != senderId) {
			Peer.getExecutor().schedule(
				new GetchunkThread(messageArray,socket), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void chunk() {

		String[] messageArray = this.message.trim().split("\\s+");

		String path = "Peer" + Peer.getId() + "/" + "CHUNK" + "/" + messageArray[2] + "/" + messageArray[3];

		if (Chunk.createChunkFile(path, Utils.getBody(messageBytes))){
			Peer.getMemory().chunksToRestore.put(messageArray[2]+ "-" + messageArray[3], messageArray[3]);
		}

	}

	private void restoreFile(){

		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != senderId) {
			System.out.println("RESTORE THREAD WORKING");
			Peer.getExecutor().schedule(
				new RestoreFileThread(messageArray[1], messageArray[2], Integer.parseInt(messageArray[3])), delay, TimeUnit.MILLISECONDS);
		}
	}

	private synchronized void delete() {

		String fileId = messageArray[2];
		Peer.getMemory().removeChunks(fileId);

		Path folderPath = Paths.get("Peer" + Peer.getId() + "/STORED/" + fileId);

		try {
			FileInfo.deleteFolder(folderPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try{
			String msg = "DELETED "+fileId+" "+Peer.getId()+" "+Peer.getMemory().availableCapacity;
			Peer.sendMessageToServer(msg);
		} catch(Exception e){}

		System.out.println("Deleted chunks of file: " + fileId);
	}

}
