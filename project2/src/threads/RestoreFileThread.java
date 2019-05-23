package threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import app.Peer;
import utils.FileInfo;

public class RestoreFileThread implements Runnable {
	private String filepath;
	private int numberChunks;

	public RestoreFileThread(String filepath, String fileId, int numberChunks) {
		this.filepath = filepath;
		this.numberChunks = numberChunks;
	}

	@Override
	public void run() {

		if (createFile())
			System.out.println("Local file restored");
		else
			System.out.println("Errror occured: Local file not created");
	}


	public boolean createFile() {

		Path restoredFilePath = Paths.get("Peer" + Peer.getId() + "/" + "RESTORED" + "/" + this.filepath);

		ArrayList<String> sortedChunks = sortChunks();
		int position = 0;
		
		if (sortedChunks.size() < this.numberChunks) {
			System.out.println(Peer.getMemory().chunksToRestore.size());
			System.out.println("Could not find all the chunks needed to restore the requested file\n");
			return false;
		} else {

			try {
				FileInfo.createFile(restoredFilePath);		


				for (String key : sortedChunks) {

					String[] splitChunkName = key.trim().split("-");
					Path chunkPath = Paths.get("Peer" + Peer.getId() + "/" + "CHUNK" + "/" + splitChunkName[0] + "/"
							+ splitChunkName[1]);

					if (!Files.exists(chunkPath)) {
						return false;
					}
					
					byte[] content = new byte[(int) Files.size(chunkPath)];

					content	= FileInfo.readFromFile(chunkPath);
					System.out.println("Chunk no " + splitChunkName[1]);

					AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(restoredFilePath, StandardOpenOption.WRITE);

					int size = (int) Files.size(chunkPath);

					ByteBuffer buffer = ByteBuffer.allocate(size);
					buffer.put(content);

					buffer.flip();

					Future<Integer> operation = fileChannel.write(buffer, position);
					operation.get();
					position += size;
			
					fileChannel.close();

					Peer.getMemory().chunksToRestore.remove(key);
				}
				return true;
			} catch (IOException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return false;

		}
	}

	public ArrayList<String> sortChunks(){
		ArrayList<String> sortedChunks = new ArrayList<String>();
		HashMap<String, String> requiredChunks = Peer.getMemory().chunksToRestore;

		for (String key : requiredChunks.keySet()) {
			sortedChunks.add(key);
	}

		sortedChunks.sort((o1, o2) -> {
			int chunk1 = Integer.valueOf(o1.split("-")[1]);
			int chunk2 = Integer.valueOf(o2.split("-")[1]);
			return Integer.compare(chunk1, chunk2);
		});

		return sortedChunks;
	}
}