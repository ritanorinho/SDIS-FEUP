package threads;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;


public class AnalizeMessageThread implements Runnable {
	String message;
	String[] messageArray;
	byte[] messageBytes;
	

	public AnalizeMessageThread(byte[] message) {
		
		this.messageBytes= message;
		this.message= new String(this.messageBytes,0,this.messageBytes.length);
		this.messageArray = this.message.trim().split("\\s+");
		// TODO Auto-generated constructor stub
	}

	private synchronized void stored() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		if (Peer.getMemory().backupChunks.containsKey(chunkId)) {
			System.out.println("replication: " + Peer.getMemory().backupChunks.get(chunkId));
			int senderId = Integer.parseInt(messageArray[2]);
			if (Peer.getId() == senderId)
				Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId) + 1);
		}
	}
	

	private synchronized void putchunk() {
		
	    String chunkId = messageArray[3] + "-" + messageArray[4];
		 System.out.println("SENDER ID: "+messageArray[2]+" PEER ID: "+Peer.getId());
		 Integer id = Integer.parseInt(messageArray[2]);
		Random random = new Random();
		int delay = random.nextInt(401);

		if (!Peer.getMemory().backupChunks.containsKey(chunkId)) {
			Peer.getMemory().backupChunks.put(chunkId, 1);
		} else {
			
				Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId) + 1);
		}
				String storedMessage = "STORED " + messageArray[1] + " " + id + " " + messageArray[3] + " "
						+ messageArray[4] + " " + "\n\r\n\r";				
				byte[] data = getBody();
				Peer.getExecutor().schedule(new StoredChunkThread(storedMessage.getBytes(),data,Integer.parseInt(messageArray[5])), delay,
						TimeUnit.MILLISECONDS);
	}

	private synchronized void delete() {

		String fileId = messageArray[3];
		;
		
		if (Peer.getMemory().hasFile(fileId)) {
			Peer.getMemory().removeChunks(fileId);
		}

	}
	private void chunk() {
		String chunkId = messageArray[3] + "-" + messageArray[4];
		byte[] body = getBody();
		int senderId = Integer.parseInt(messageArray[2]);
		
		if (Peer.getId() == senderId) {
			
			String chunkPath =  "Peer"+Peer.getId() +"/"+"CHUNK"+"/"+messageArray[3]+"/"+messageArray[4];
			File chunkFile = new File(chunkPath);		
			try {		
				if (!chunkFile.exists()) {
					chunkFile.getParentFile().mkdirs();
						chunkFile.createNewFile();
				}
			byte[] content = getBody();
			FileOutputStream fos;
				fos = new FileOutputStream(chunkFile);
				System.out.println("Chunk no "+messageArray[4]+ " content  " +content.length);
				fos.write(content);	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			Peer.getMemory().requiredChunks.put(chunkId, messageArray[3]);
		}
					
	}

	private void getchunk() {
		String[] messageArray = this.message.trim().split("\\s+");
		String chunkId = messageArray[3] + "-" + messageArray[4];
		
		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);
		if (Peer.getId() != senderId && Peer.getMemory().backupChunks.containsKey(chunkId))
		{
			Peer.getExecutor().schedule(new GetchunkThread(messageArray), delay,
					TimeUnit.MILLISECONDS);
		}

	}

	@Override
	public void run() {
		String messageType = this.message.trim().split("\\s+")[0];
		switch (messageType) {
		case "PUTCHUNK":
			putchunk();
			break;
		case "STORED":
			stored();
			break;
		case "DELETE":
			delete();
			break;
		case "GETCHUNK":
			getchunk();
			break;
		case "CHUNK":
			chunk();
			break;
		case "REMOVED":
			removed();
		default:
		}
	}

	private void removed() {
			String fileId = this.messageArray[3];
			String chunkNo = this.messageArray[4];
			String chunkId = fileId +"-"+chunkNo;
			int senderId = Integer.parseInt(this.messageArray[2]);
			if (senderId != Peer.getId()) {
				Peer.getMemory().savedOcurrences.put(chunkId,Peer.getMemory().savedOcurrences.get(chunkId)-1);
				Random random = new Random();
				int delay = random.nextInt(401);
				Peer.getExecutor().schedule(new RemovedChunkThread(chunkId), delay, TimeUnit.MILLISECONDS);
			}
		
		
	}

	private byte[] getBody() {
		int i;
		for (i =0; i< this.messageBytes.length-4;i++) {
			if (this.messageBytes[i] == 0xA && this.messageBytes[i+1]== 0xD && this.messageBytes[i+2]== 0xA && this.messageBytes[i+3]== 0xD) {
				break;
			}
			
			
		}
		int j = this.messageBytes.length -(i+4);
		
		byte[] body = Arrays.copyOfRange(this.messageBytes,i+4,this.messageBytes.length);
		System.out.println(this.messageBytes.length);
		System.out.println("body "+body.length);
		
		return body;
	}

}
