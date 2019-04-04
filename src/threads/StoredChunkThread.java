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
	private int replicationDegree;
	String msg;
	
	public StoredChunkThread(byte[] storedMessage, byte[]data, int replicationDegree) {
		this.byteMessage=storedMessage;
		this.data=data;
		this.msg = new String(this.byteMessage, 0, this.byteMessage.length);
		this.messageArray= msg.split("\\s+");
		this.senderId = Integer.parseInt(messageArray[2]);
		this.replicationDegree=replicationDegree;
	}
	
	
	
	
	private void createFileChunk() {
		String filename = "Peer"+Peer.getId() +"/"+messageArray[0]+"/"+messageArray[3]+"/"+messageArray[4];
		System.out.println(filename);
		try {
			if (Peer.getId() != this.senderId) {
			File file = new File(filename);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename);
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
			
			if (Peer.getMemory().files.get(i).getFileId().equals(messageArray[3])) {
				return;
			}
				
		}
		String chunkName = this.messageArray[3]+"-"+this.messageArray[4];
		Chunk chunk = new Chunk(this.messageArray[3],Integer.parseInt(this.messageArray[4]),this.data,this.data.length,chunkName,this.replicationDegree);
		if (Peer.getId() != senderId && !Peer.getMemory().savedChunks.containsKey(chunkName)) {
		Peer.getMemory().savedChunks.put(chunkName, chunk);
		Peer.getMemory().updateMemoryUsed(this.data.length);
		createFileChunk();
		try {
			Peer.getMCListener().message(byteMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		
		}		

	@Override
	public void run() {
		if(Peer.getMemory().getAvailableCapacity()>=this.data.length) {			
			int senderId = Integer.parseInt(messageArray[2]);
			if (Peer.getId() != senderId) {
		
				saveChunk();
					
			}
		}
			else {
				System.out.println("There isn't enough disk space to save this chunk\n");
				return;
			}
		
	}

}
