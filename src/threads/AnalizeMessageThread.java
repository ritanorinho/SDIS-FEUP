package threads;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class AnalizeMessageThread implements Runnable {
	String message;

	public AnalizeMessageThread(String msg) {
		this.message = msg;
		// TODO Auto-generated constructor stub
	}

	private synchronized void stored() {
		String[] messageArray = this.message.trim().split("\\s+");
		String chunkId = messageArray[3] + "-" + messageArray[4];
		if (Peer.getMemory().backupChunks.containsKey(chunkId)) {
			System.out.println("replication: " + Peer.getMemory().backupChunks.get(chunkId));

			Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId) + 1);
		}

	}

	private synchronized void putchunk() {

		String[] messageArray = this.message.trim().split("\\s+");
		String chunkId = messageArray[3] + "-" + messageArray[4];
		// System.out.println("SENDER ID: "+messageArray[2]+" PEER ID: "+Peer.getId());
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
				Peer.getExecutor().schedule(new StoredChunkThread(storedMessage.getBytes()), delay,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	private synchronized void delete() {

		String[] messageArray = this.message.trim().split("\\s+");
		String fileId = messageArray[3];
		;
		
		if (Peer.getMemory().hasFile(fileId)) {
			Peer.getMemory().removeChunks(fileId);
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
		default:
		}
	}

	private void getchunk() {
		String[] messageArray = this.message.trim().split("\\s+");
		String chunkId = messageArray[3] + "-" + messageArray[4];
		System.out.println(chunkId);
		String restoredChunk = "CHUNK " + messageArray[0] + " " + messageArray[1] + " " + messageArray[2] + " "
				+ messageArray[3] + "\n\r\n\r";
		Random random = new Random();
		int delay = random.nextInt(401);
		if (Peer.getMemory().backupChunks.containsKey(chunkId)) {
			Peer.getExecutor().schedule(new RestoredChunkThread(restoredChunk.getBytes()), delay,
					TimeUnit.MILLISECONDS);
		}

	}
}
