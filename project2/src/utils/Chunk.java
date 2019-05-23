package utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

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

	public static boolean createChunkFile(String path, byte[] data) {
		Path filePath = Paths.get(path);

		try {
			if (!Files.exists(filePath)) {
				filePath.getParent().toFile().mkdirs();
				Files.createFile(filePath);
			}

			AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(filePath, StandardOpenOption.WRITE);

			ByteBuffer buffer = ByteBuffer.allocate(FileInfo.MAX_SIZE);
			buffer.put(data);
			buffer.flip();

			fileChannel.write(buffer, 0).get();
			
			fileChannel.close();

		} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}

		return true;


	}

}
