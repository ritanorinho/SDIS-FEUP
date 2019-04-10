package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javafx.util.Pair;
import project.Peer;
import utils.FileInfo;

public class AnalizeMessageThread implements Runnable {
	String message;
	String[] messageArray;
	byte[] messageBytes;
	String chunkId;
	InetAddress InetAddress;

	public AnalizeMessageThread(byte[] message) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = this.message.trim().split("\\s+");

		if(messageArray.length>4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else this.chunkId = this.messageArray[3];
	}

	public AnalizeMessageThread(byte[] message, InetAddress adress) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = this.message.trim().split("\\s+");
		if(messageArray.length>4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else this.chunkId = this.messageArray[3];
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
		case "REMOVED":
			removed();
			break;
		case "CONFIRMCHUNK":
			confirmChunk();
			break;
		default:
		}
	}

	private synchronized void stored() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		int senderId = Integer.parseInt(messageArray[2]);
		if (Peer.getMemory().savedOcurrences.containsKey(chunkId) && Peer.getId() != senderId) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) + 1);
		}
	}

	private synchronized void putchunk() {

		System.out.println("SENDER ID: " + messageArray[2] + " PEER ID: " + Peer.getId());
		Integer id = Integer.parseInt(messageArray[2]);
		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != id && !Peer.getMemory().savedChunks.containsKey(chunkId)) {
			if (!Peer.getMemory().savedOcurrences.containsKey(chunkId)) {
				Peer.getMemory().savedOcurrences.put(chunkId, 0);
			}
			String storedMessage = messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
					+ messageArray[4];
			System.out.println(storedMessage);
			byte[] data = getBody();
			Peer.getExecutor().schedule(
					new StoredChunkThread(storedMessage.getBytes(), data, Integer.parseInt(messageArray[5])), delay,
					TimeUnit.MILLISECONDS);
		}
	}

	private synchronized void delete() {

		String fileId = messageArray[3];
		Peer.getMemory().removeChunks(fileId);

		String localDirPath = "Peer" + Peer.getId() + "/STORED/" + fileId;
		File localDir = new File(localDirPath);
		FileInfo.deleteFolder(localDir);
	}

	private void chunk() {

		int senderId = Integer.parseInt(messageArray[2]);

		if (Peer.getId() == senderId) {

			String chunkPath = "Peer" + Peer.getId() + "/" + "CHUNK" + "/" + messageArray[3] + "/" + messageArray[4];
			File chunkFile = new File(chunkPath);
			try {
				if (!chunkFile.exists()) {
					chunkFile.getParentFile().mkdirs();
					chunkFile.createNewFile();
				}
				byte[] content = getBody();
				FileOutputStream fos;
				fos = new FileOutputStream(chunkFile);
				fos.write(content);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Peer.getMemory().chunksToRestore.put(chunkId, messageArray[3]);
		}

	}

	private void getchunk() {

		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);
		if (Peer.getId() != senderId && Peer.getMemory().savedChunks.containsKey(chunkId)) {
			Peer.getExecutor().schedule(new GetchunkThread(messageArray), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void removed() {

		int senderId = Integer.parseInt(this.messageArray[2].trim());
		if (senderId != Peer.getId()) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) - 1);

			Random random = new Random();
			int delay = random.nextInt(401);
			Peer.getExecutor().schedule(new RemovedChunkThread(chunkId), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void confirmChunk() {
		int senderId = Integer.parseInt(this.messageArray[2].trim());
		String chunkid = messageArray[3].trim();
		int port = Integer.parseInt(this.messageArray[4].trim());
		if (senderId != Peer.getId() && !Peer.getMemory().confirmedChunks.containsKey(chunkid)) {
		
			Peer.getMemory().confirmedChunks.put(chunkid, new Pair<>(port,this.InetAddress));
		}
	
}

	private byte[] getBody() {
		int i;
		for (i =0; i< this.messageBytes.length-4;i++) {
			if (this.messageBytes[i] == 0xD && this.messageBytes[i+1]== 0xA && this.messageBytes[i+2]== 0xD && this.messageBytes[i+3]== 0xA) {
				break;
			}
		}
		
		byte[] body = Arrays.copyOfRange(this.messageBytes,i+4,this.messageBytes.length);		
		return body;
	}

}
