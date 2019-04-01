package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import project.Peer;
import utils.Chunk;

public class StoredChunkThread implements Runnable {
	byte[] byteMessage;
	byte[] data;
	String[] messageArray;
	private int senderId;
	
	public StoredChunkThread(byte[] storedMessage, byte[]data) {
		this.byteMessage=storedMessage;
		this.data=data;
		String msg = new String(this.byteMessage, 0, this.byteMessage.length);
		this.messageArray= msg.split("\\s+");
		this.senderId = Integer.parseInt(messageArray[2]);
		System.out.println("SENDER ID "+this.senderId);
		
		
		 
	}
	
	
	
	private void createFileChunk() {
		
		String filename = "Peer"+Peer.getId() +"/"+messageArray[0]+"/"+messageArray[3]+"/"+messageArray[4];
		
		try {
			if (Peer.getId() != this.senderId) {
			File file = new File(filename);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename);
			System.out.println("STORED: "+this.data.length);
			fos.write(this.data);
			fos.close();
			}
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
		String chunkName = this.messageArray[3]+"-"+this.messageArray[4];
		Chunk chunk = new Chunk(this.messageArray[3],Integer.parseInt(this.messageArray[4]),this.data,this.data.length,chunkName);
		Peer.getMemory().savedChunks.put(chunkName, chunk);
		System.out.println(Peer.getMemory().savedChunks);
		if (!Peer.getMemory().savedOcurrences.containsKey(chunkName)) {
			Peer.getMemory().savedOcurrences.put(chunkName,1);
		}
		if (Peer.getId() == senderId) {
			Peer.getMemory().savedOcurrences.put(chunkName,Peer.getMemory().savedOcurrences.get(chunkName)+1);
		}
		
	}

	@Override
	public void run() {
		try {
			if(Peer.getMemory().getAvailableCapacity()>=this.data.length) {
				Peer.getMemory().updateMemoryUsed(this.data.length);
				saveChunk();
				createFileChunk();
				}
				else {
					System.out.println("There isn't enough disk space to save this chunk\n");
					return;
				}
			Peer.getMCListener().message(byteMessage);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
