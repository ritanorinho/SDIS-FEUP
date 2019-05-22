package threads;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import app.Peer;
import utils.Chunk;
import utils.FileInfo;
import utils.Pair;
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

	public AnalizeMessageThread(byte[] message, InetAddress adress) {

		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = Utils.byteArrayToStringArray(message);

		if (messageArray.length > 4)
			this.chunkId = this.messageArray[2] + "-" + this.messageArray[3];
		else if (messageArray.length > 3)
			this.chunkId = this.messageArray[2];
		this.InetAddress = adress;

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
		case "CONFIRMCHUNK":
			confirmChunk();
			break;
		case "ALIVE":
			alive();
			break;
		default:
		}
	}

	private void alive() {
		int senderId = Integer.parseInt(this.messageArray[1]);
		if (senderId != Peer.getId()) {
			for (int i = 0; i < Peer.getMemory().deletedFiles.size(); i++) {
				String deletedMessage = "DELETE " + this.messageArray[1] + " " + Peer.getId() + " "
						+ Peer.getMemory().deletedFiles.get(i) + " " + "\r\n\r\n";
				System.out.println("\n SENT " + deletedMessage);
			}
		}

	}

	private synchronized void stored() {
		String chunkId = messageArray[2] + "-" + messageArray[3];
		if (Peer.getMemory().savedOcurrences.containsKey(chunkId) && Peer.getId() != senderId) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) + 1);
			Utils.savedOccurrencesFile();
		}

		try {
			
			Peer.sendMessageToServer("STORED " +  messageArray[1] + " "+chunkId);
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
				Utils.savedOccurrencesFile();
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
	System.out.println("RECEIVING GETCHUNK");
		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != senderId) {
			Peer.getExecutor().schedule(
				new GetchunkThread(messageArray,socket), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void chunk() {
		System.out.println("RECEIVING CHUNK");
		String[] messageArray = this.message.trim().split("\\s+");

		if (Chunk.processChunk(this.messageBytes, Peer.getId())){
			Peer.getMemory().chunksToRestore.put(messageArray[2]+ "-" + messageArray[3], messageArray[3]);
		}

	}

	private void restoreFile(){
		System.out.println("RESTORE THREAD IS HERE");
		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != senderId) {
			System.out.println("RESTORE THREAD WORKING");
			Peer.getExecutor().schedule(
				new RestoreFileThread(messageArray[1], messageArray[2], Integer.parseInt(messageArray[3]),1.0), delay, TimeUnit.MILLISECONDS);
		}
	}

	private synchronized void delete() {

		String fileId = messageArray[2];
		Peer.getMemory().removeChunks(fileId);
		String localDirPath = "Peer" + Peer.getId() + "/STORED/" + fileId;
		File localDir = new File(localDirPath);
		FileInfo.deleteFolder(localDir);
		try{
		String msg = "DELETED "+fileId+" "+Peer.getId();
		Peer.sendMessageToServer(msg);
		}
		catch(Exception e){

		}
		System.out.println("Deleted chunks of file: " + fileId);
	}

	private void confirmChunk() {

		String chunkid = messageArray[3].trim();
		int port = Integer.parseInt(this.messageArray[4].trim());
		if (senderId != Peer.getId() && !Peer.getMemory().confirmedChunks.containsKey(chunkid)) {
			Peer.getMemory().confirmedChunks.put(chunkid, new Pair<Integer, InetAddress>(port, this.InetAddress));
		}

	}

}
