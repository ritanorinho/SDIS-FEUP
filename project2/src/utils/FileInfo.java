package utils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FileInfo implements Serializable{

	private static final long serialVersionUID = 1L;
	public static int MAX_SIZE = 16000;
	public static int MAX_SIZE_FILE = 6400000;

	private String fileId;
	private ArrayList<Chunk> chunks = new ArrayList<Chunk>();
	private transient Path filePath = null;
	private String stringPath;
	private int replicationDegree;

	public FileInfo(String filePath, int repDegree) {
		this.stringPath = filePath;
		this.filePath = Paths.get(filePath);
		this.replicationDegree = repDegree;
		fileId();
		calculateNumberChunks();
	}

	public void checkPath(){
		if(filePath==null)
			filePath = Paths.get(this.stringPath);
	}

	public void calculateNumberChunks() {

		checkPath();

		int position = 0;
		int bytesRead = 0;
		int chunksCount = 0;
		String chunkId;
		byte[] content = new byte[MAX_SIZE];
		AsynchronousFileChannel fileChannel;
		try {
			fileChannel = AsynchronousFileChannel.open(filePath, StandardOpenOption.READ);
			ByteBuffer buffer = ByteBuffer.allocate(MAX_SIZE);
			Future<Integer> operation = fileChannel.read(buffer, position);

			try {
				while ((bytesRead = operation.get()) > 0) {
					content = buffer.array();
					chunksCount++;
					position += MAX_SIZE;
					byte[] body = Arrays.copyOf(content, bytesRead);
					chunkId = this.fileId + "-" + chunksCount;
					this.chunks
							.add(new Chunk(this.fileId, chunksCount, body, bytesRead, chunkId, this.replicationDegree));
					buffer.clear();
					operation = fileChannel.read(buffer, position);

				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			fileChannel.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			if (Files.size(this.filePath) % 16000 == 0) {
				chunkId = this.fileId + "-" + chunksCount;
				this.chunks.add(new Chunk(this.fileId, chunksCount, null, 0, chunkId, this.replicationDegree));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		
	
	}
	
	public void fileId() {
		checkPath();
		this.fileId = Utils.createFileId(this.filePath);	
	}

	public static void deleteFolder(Path folderPath) throws IOException {
		
		if (Files.isDirectory(folderPath, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(folderPath)) {
			  for (Path entry : entries) {
				deleteFolder(entry);
			  }
			}
		  }
		Files.delete(folderPath);
	}
	
	public ArrayList<Chunk> getChunks(){ return this.chunks; }

	public String getFileId() { return this.fileId;}

	public String getFilename() { 
		checkPath();
		return this.filePath.getFileName().toString(); 
	}

	public String getFilePath() { 
		checkPath();
		return this.filePath.toString(); 
	}

	public int getReplicationDegree() { return this.replicationDegree; }

	public static void createFile(Path path) throws IOException {
		if (!Files.exists(path)) {
			path.getParent().toFile().mkdirs();
			Files.createFile(path);
		}
	}

	
	public static byte[] readFromFile(Path path) throws IOException, InterruptedException, ExecutionException {

		AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

		ByteBuffer buffer = ByteBuffer.allocate((int) Files.size(path));

		Future<Integer> operation = fileChannel.read(buffer, 0);
		operation.get();

		buffer.clear();
		fileChannel.close();

		return buffer.array();
	}

	

}
