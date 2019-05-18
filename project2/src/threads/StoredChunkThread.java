package threads;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


import project.Peer;
import utils.Chunk;

public class StoredChunkThread implements Runnable {
	byte[] byteMessage;
	byte[] data;
	String[] messageArray;
	private int replicationDegree;
	String msg;
	private String version;
	private String fileId;
	private String chunkNo;
	private String chunkId;
	private int senderId;
	private Socket socket;
	

	public StoredChunkThread(byte[] storedMessage, byte[] data, int replicationDegree,Socket socket) {
		this.byteMessage = storedMessage;
		this.data = data;
		this.msg = new String(this.byteMessage, 0, this.byteMessage.length);
		this.messageArray = msg.split("\\s+");
		this.version = messageArray[0];
		this.senderId = Integer.parseInt(messageArray[1]);
		this.fileId = messageArray[2];
		this.chunkNo = messageArray[3];
		this.chunkId = this.fileId + "-" + this.chunkNo;
		this.replicationDegree = replicationDegree;
		this.socket = socket;
	}

	private void createFileChunk() {
		String filename = "Peer" + Peer.getId() + "/" + "STORED" + "/" + this.fileId + "/" + this.chunkNo + "-"
				+ this.replicationDegree;

		try {

			File file = new File(filename);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(this.data);
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveChunk() {

		Chunk chunk = new Chunk(this.fileId, Integer.parseInt(this.chunkNo), this.data, this.data.length, this.chunkId,
				this.replicationDegree);

		if (!Peer.getMemory().savedChunks.containsKey(this.chunkId)) {
			Peer.getMemory().savedChunks.put(this.chunkId, chunk);
			Peer.getMemory().updateMemoryUsed(this.data.length);
			createFileChunk();

				String storedMessage = "STORED " + this.version + " " + Peer.getId() + " " + this.fileId + " "
						+ this.chunkNo + "\r\n\r\n";
				System.out.println("\nSENT " + storedMessage);
				try{
				OutputStream outputStream = socket.getOutputStream();
				DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
				dataOutputStream.writeInt(storedMessage.getBytes().length);
				dataOutputStream.write(storedMessage.getBytes());
				}
				catch(Exception e){

				}


		} else {
			return;
		}
	}

	@Override
	public void run() {
		for (int i = 0; i < Peer.getMemory().files.size(); i++) {
			if (Peer.getMemory().files.get(i).getFileId().equals(fileId))
				return;
		}
		if (version.equals("1.1")) {
			try {
				Thread.sleep((long) (Math.random() * 1500));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Peer.getMemory().savedOcurrences.get(this.chunkId) >= this.replicationDegree) {
				System.out.println("CHUNK NO: " + chunkNo + ": Replication degree rechead");
				return;
			}

		}
		if (Peer.getId() == this.senderId)
			return;
		if (Peer.getMemory().getAvailableCapacity() >= this.data.length) {
			saveChunk();
		} else {
			System.out.println("There isn't enough disk space to save this chunk\n");
			return;
		}
	}
}
