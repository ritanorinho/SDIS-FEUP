package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import project.Peer;
import utils.Chunk;

public class StoredChunkThread implements Runnable {
	byte[] byteMessage;
	String content;
	String[] messageArray;
	
	public StoredChunkThread(byte[] storedMessage, String message) {
		this.byteMessage=storedMessage;
		this.content=message;
		String msg = new String(this.byteMessage, 0, this.byteMessage.length);
		this.messageArray= msg.split("\\s+");
		saveChunk();
		createFileChunk();
		 
	}

	private void createFileChunk() {
		
		String filename = "Peer"+Peer.getId() +"/"+messageArray[0]+"/"+messageArray[3]+"-"+messageArray[4];
	
		try {
			File file = new File(filename);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(this.content.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		// TODO Auto-generated method stub
		
	}

	private void saveChunk() {
		for(int i = 0;i< Peer.getMemory().files.size();i++) {
			if (Peer.getMemory().files.get(i).getFileId().equals(messageArray[2]))
		return;
		}
		Chunk chunk = new Chunk(this.messageArray[3],Integer.parseInt(this.messageArray[4]),this.content.getBytes(),this.content.length());
		String chunkName = this.messageArray[3]+"-"+this.messageArray[4];
		Peer.getMemory().savedChunks.put(chunkName, chunk);
		
	}

	@Override
	public void run() {
		try {
			Peer.getMCListener().message(byteMessage);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
