package threads;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javafx.util.Pair;
import project.Peer;
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

	public AnalizeMessageThread(byte[] message) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = Utils.byteArrayToStringArray(message);

		if (messageArray.length > 4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else
			this.chunkId = this.messageArray[3];

		this.senderId = Integer.parseInt(this.messageArray[2].trim());
	}

	public AnalizeMessageThread(byte[] message, InetAddress adress) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = this.message.trim().split("\\s+");
		if (messageArray.length > 4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else
			this.chunkId = this.messageArray[3];
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
		if (Peer.getMemory().savedOcurrences.containsKey(chunkId) && Peer.getId() != senderId) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) + 1);
			Utils.savedOccurrencesFile();
		}
	}

	private synchronized void putchunk() {

		Integer id = Integer.parseInt(messageArray[2]);
		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != id && !Peer.getMemory().savedChunks.containsKey(chunkId)) {
			if (!Peer.getMemory().savedOcurrences.containsKey(chunkId)) {
				Peer.getMemory().savedOcurrences.put(chunkId, 0);
				Utils.savedOccurrencesFile();
			}
			String storedMessage = messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
					+ messageArray[4];
			System.out.println("SENDER ID: " + messageArray[2] + " PEER ID: " + Peer.getId() + "\n" + storedMessage);
			byte[] data = Utils.getBody(this.messageBytes);
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

		System.out.println("Deleted chunks of file: " + fileId);
	}

	private void chunk() {

		if (Peer.getId() == senderId) {

			if (Chunk.processChunk(this.messageBytes, Peer.getId()))
				Peer.getMemory().chunksToRestore.put(chunkId, messageArray[3]);
		}

	}

	private void getchunk() {

		String[] messageArray = this.message.trim().split("\\s+");

		Random random = new Random();
		int delay = random.nextInt(401);

		if (Peer.getId() != senderId && Peer.getMemory().savedChunks.containsKey(chunkId)) {
			Peer.getExecutor().schedule(new GetchunkThread(messageArray), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void removed() {

		if (senderId != Peer.getId()) {
			Peer.getMemory().savedOcurrences.put(chunkId, Peer.getMemory().savedOcurrences.get(chunkId) - 1);
			Utils.savedOccurrencesFile();

			Random random = new Random();
			int delay = random.nextInt(401);
			Peer.getExecutor().schedule(new RemovedChunkThread(chunkId), delay, TimeUnit.MILLISECONDS);
		}

	}

	private void confirmChunk() {

		String chunkid = messageArray[3].trim();
		int port = Integer.parseInt(this.messageArray[4].trim());
		if (senderId != Peer.getId() && !Peer.getMemory().confirmedChunks.containsKey(chunkid)) {

			Peer.getMemory().confirmedChunks.put(chunkid, new Pair<>(port, this.InetAddress));
		}

	}

}
