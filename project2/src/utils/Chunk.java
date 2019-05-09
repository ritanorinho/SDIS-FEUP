package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Chunk {

	private String fileId;
	private int chunkNo;
	private byte[] data;
	private int length;
	private String chunkId;
	private int replicationDegree;

	public Chunk(String fileId, int chunkNo, byte[] data, int length, String chunkId, int replicationDegree) {
		this.chunkNo = chunkNo;
		this.data = data;
		this.length = length;
		this.fileId = fileId;
		this.chunkId = chunkId;
		this.replicationDegree = replicationDegree;
	}

	public String getFileId() {
		return fileId;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public byte[] getData() {
		return data;
	}

	public int getLength() {
		return length;
	}

	public String getChunkId() {
		return this.chunkId;
	}

	public int getChunkSize() {
		return this.length;
	}

	public int getReplicationDegree() {
		return this.replicationDegree;
	}

	public static boolean processChunk(byte[] messageBytes, int peerID) {

		String[] messageArray = Utils.byteArrayToStringArray(messageBytes);

		String chunkPath = "Peer" + peerID + "/" + "CHUNK" + "/" + messageArray[3] + "/" + messageArray[4];
		File chunkFile = new File(chunkPath);
		try {
			if (!chunkFile.exists()) {
				chunkFile.getParentFile().mkdirs();
				chunkFile.createNewFile();
			}
			byte[] content = Utils.getBody(messageBytes);
			FileOutputStream fos;
			fos = new FileOutputStream(chunkFile);
			fos.write(content);
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
