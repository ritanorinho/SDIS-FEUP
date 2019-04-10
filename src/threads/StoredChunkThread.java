package threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import project.Peer;
import utils.Chunk;
import utils.Utils;

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
	
	
	public StoredChunkThread(byte[] storedMessage, byte[]data, int replicationDegree) {
		this.byteMessage=storedMessage;
		this.data=data;
		this.msg = new String(this.byteMessage, 0, this.byteMessage.length);
		this.messageArray= msg.split("\\s+");
		this.version = messageArray[0];
		this.senderId = Integer.parseInt(messageArray[1]);
		this.fileId = messageArray[2];
		this.chunkNo = messageArray[3];
		this.chunkId = this.fileId+"-"+this.chunkNo;
		this.replicationDegree=replicationDegree;
	}
	

	private  void createFileChunk() {
		String filename = "Peer"+Peer.getId() +"/"+"STORED"+"/"+this.fileId+"/"+this.chunkNo+"-"+this.replicationDegree;
		System.out.println(filename);
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

	private  void saveChunk() {
		
		System.out.println(Peer.getMemory().savedOcurrences.get(this.chunkId)+" "+this.replicationDegree);

		if (version.equals("2.0") && Peer.getMemory().savedOcurrences.get(this.chunkId) >= this.replicationDegree) {
			System.out.println("new version");
			System.out.println("Replication degree rechead");
			return;
		}

		Chunk chunk = new Chunk(this.fileId,Integer.parseInt(this.chunkNo),this.data,this.data.length,this.chunkId,this.replicationDegree);

		if (!Peer.getMemory().savedChunks.containsKey(this.chunkId)) {
		Peer.getMemory().savedChunks.put(this.chunkId, chunk);
		Peer.getMemory().savedOcurrences.put(this.chunkId, Peer.getMemory().savedOcurrences.get(this.chunkId) + 1);
		Utils.savedOccurrencesFile();
		Peer.getMemory().updateMemoryUsed(this.data.length);	
		
		createFileChunk();
		
		try {
			String storedMessage = "STORED "+this.version+" "+Peer.getId()+" "+this.fileId+" "+this.chunkNo+"\n\r\n\r";
			System.out.println(storedMessage);
			Peer.getMCListener().message(storedMessage.getBytes("US-ASCII"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		else return;
		}
			
			createFileChunk();
			
			try {
				String storedMessage = "STORED "+this.version+" "+Peer.getId()+" "+this.fileId+" "+this.chunkNo+"\n\r\n\r";
				System.out.println(storedMessage);
				Peer.getMCListener().message(storedMessage.getBytes("US-ASCII"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else return;
	}
			

	@Override
	public void run() {
	
		try {
			Thread.sleep((long)(Math.random() * 1500));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			if (Peer.getId()==this.senderId) return;
			if(Peer.getMemory().getAvailableCapacity()>=this.data.length) {	
					saveChunk();					
			}
				else {
					System.out.println("There isn't enough disk space to save this chunk\n");
					return;
				}
		}
	}

}
		
