package threads;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;
import project.Server;
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
	double version;
	Socket socket;


	public AnalizeMessageThread(byte[] message,Socket socket) {
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = Utils.byteArrayToStringArray(message);
		this.version = Double.parseDouble(messageArray[1]);
		this.socket = socket;

		if (messageArray.length > 4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else if (messageArray.length > 3)this.chunkId = this.messageArray[3];
	}

	public AnalizeMessageThread(byte[] message, InetAddress adress) {
		
		this.messageBytes = message;
		this.message = new String(this.messageBytes, 0, this.messageBytes.length);
		this.messageArray = Utils.byteArrayToStringArray(message);
		this.version = Double.parseDouble(messageArray[1]);

		if (messageArray.length > 4)
			this.chunkId = this.messageArray[3] + "-" + this.messageArray[4];
		else if (messageArray.length > 3) this.chunkId = this.messageArray[3];
		this.InetAddress = adress;
		

	}

	@Override
	public void run() {
		String messageType = this.message.trim().split("\\s+")[0];

		if(!isVersionSupported()){
			return;
		}

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
		case "ALIVE":
			alive();
			break;
		default:
		}
	}

	private boolean isVersionSupported(){
		if(this.version > Peer.getProtocolVersion())
			return false;
		return true;
	}

	private void alive() {
		int senderId = Integer.parseInt(this.messageArray[2]);
		if (senderId != Peer.getId()) {
			for (int i = 0;i< Peer.getMemory().deletedFiles.size();i++) {
				String deletedMessage = "DELETE " + this.messageArray[1] + " " + Peer.getId() + " " + Peer.getMemory().deletedFiles.get(i) + " " + "\r\n\r\n";
				System.out.println("\n SENT "+deletedMessage);
				try {
					Peer.getExecutor().execute(new WorkerThread(deletedMessage.getBytes("US-ASCII"),"mc"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
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
			byte[] data = Utils.getBody(this.messageBytes);
			Peer.getExecutor().schedule(
					new StoredChunkThread(storedMessage.getBytes(), data, Integer.parseInt(messageArray[5]),socket), delay,
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
		
			if (Chunk.processChunk(this.messageBytes, Peer.getId()))
				Peer.getMemory().chunksToRestore.put(chunkId, messageArray[3]);

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
			Peer.getMemory().confirmedChunks.put(chunkid, new Pair(port, this.InetAddress));
		}

	}

}
