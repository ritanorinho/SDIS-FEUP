package threads;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import project.Peer;
import utils.Chunk;
import utils.Utils;

public class RestoreFileThread implements Runnable {
	private String filename;
	private String fileId;
	private int numberChunks;
	private Socket socket;
	private double workingVersion;

	public RestoreFileThread(String filename, String fileId, int numberChunks, double workingVersion) {
		this.filename = filename;
		this.fileId = fileId;
		this.numberChunks = numberChunks;
		this.workingVersion = workingVersion;
	}

	@Override
	public void run() {

		if (workingVersion == 1.1)
			getChunks();

		if (createFile())
			System.out.println("Local file restored");
		else
			System.out.println("Errror occured: Local file not created");
	}

	public void getChunks() {
		String chunkId;

		for (int i = 1; i <= numberChunks; i++) {
			chunkId = fileId + "-" + i;
			int port = Peer.getMemory().confirmedChunks.get(chunkId).getKey();
			InetAddress InetAddress = Peer.getMemory().confirmedChunks.get(chunkId).getValue();
			System.out.println(
					"for chunk no" + i + " connect to port" + ": " + Peer.getMemory().confirmedChunks.get(chunkId).getKey());

			try {
				// new socket
				socket = new Socket(InetAddress, port);

				// input stream
				InputStream inputStream = socket.getInputStream();
				DataInputStream dataInputStream = new DataInputStream(inputStream);

				// read messages
				int length = dataInputStream.readInt();
				byte[] data = new byte[length];
				dataInputStream.read(data, 0, length);

				// mark chunk to be restored
				markChunk(chunkId, data);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void markChunk(String chunkId, byte[] received) {
		String[] receivedStrings = Utils.byteArrayToStringArray(received);

		if (Chunk.processChunk(received, Peer.getId()))
			Peer.getMemory().chunksToRestore.put(chunkId, receivedStrings[3]);

	}

	public boolean createFile() {

		String filename = "Peer" + Peer.getId() + "/" + "RESTORED" + "/" + this.filename;
		File finalFile = new File(filename);
		HashMap<String, String> requiredChunks = Peer.getMemory().chunksToRestore;
		ArrayList<String> sortedChunks = new ArrayList<String>();
		for (String key : requiredChunks.keySet()) {
			if (requiredChunks.get(key).equals(this.fileId))
				sortedChunks.add(key);
		}

		sortedChunks.sort((o1, o2) -> {
			int chunk1 = Integer.valueOf(o1.split("-")[1]);
			int chunk2 = Integer.valueOf(o2.split("-")[1]);
			return Integer.compare(chunk1, chunk2);
		});
		
		if (sortedChunks.size() < this.numberChunks) {
			System.out.println("Could not find all the chunks needed to restore the requested file\n");
			return false;
		} else {
			try {
				if (!finalFile.exists()) {
					finalFile.getParentFile().mkdirs();
					finalFile.createNewFile();
				}		

				@SuppressWarnings("resource")
				FileOutputStream fos = new FileOutputStream(finalFile);
				for (String key : sortedChunks) {

					String[] splitChunkName = key.trim().split("-");
					String chunkPath = "Peer" + Peer.getId() + "/" + "CHUNK" + "/" + splitChunkName[0] + "/"
							+ splitChunkName[1];
					File chunkFile = new File(chunkPath);
					if (!chunkFile.exists()) {
						return false;
					}
					byte[] content = new byte[(int) chunkFile.length()];

					FileInputStream in = new FileInputStream(chunkFile);
					in.read(content);
					System.out.println("Chunk no " + splitChunkName[1] + " content  " + content.length);
					fos.write(content);
					in.close();
					Peer.getMemory().chunksToRestore.remove(key);
				}
				fos.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;

		}
	}
}