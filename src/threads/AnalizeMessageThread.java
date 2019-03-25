package threads;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import project.Peer;

public class AnalizeMessageThread implements Runnable {
	String message ;
	 public AnalizeMessageThread(String msg) {
		 this.message=msg;
		// TODO Auto-generated constructor stub
	}
		private synchronized void stored() {
			String[] messageArray = this.message.trim().split("\\s+");
			String chunkId = messageArray[3]+"-" +messageArray[4];
			Integer id = Integer.parseInt(messageArray[2]);
			if(!Peer.getMemory().backupChunks.containsKey(chunkId)) {
				Peer.getMemory().backupChunks.put(chunkId, 0);
			}
			else {
				if (Peer.getId() != id ) {
				Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId)+1);
				
				System.out.println(Peer.getMemory().backupChunks);
				}
			}
			
		}
		private synchronized void putchunk() {
			
			String[] messageArray = this.message.trim().split("\\s+");
			String chunkId = messageArray[3]+"-" +messageArray[4];
			System.out.println("SENDER ID: "+messageArray[2]+"PEER ID: "+Peer.getId());
			Integer id = Integer.parseInt(messageArray[2]);
			Random random = new Random();
			int delay = random.nextInt(401);
			if(!Peer.getMemory().backupChunks.containsKey(chunkId)) {
				Peer.getMemory().backupChunks.put(chunkId, 0);
			}
			else {
				if (Peer.getId() != id) {
				Peer.getMemory().backupChunks.put(chunkId, Peer.getMemory().backupChunks.get(chunkId)+1);
				System.out.println(Peer.getMemory().backupChunks);
				String storedMessage = "STORED "+messageArray[1]+" "+ id + " "+messageArray[4]+"\n\r\n\r";
				Peer.getExecutor().schedule(new StoredChunkThread(storedMessage.getBytes()),delay, TimeUnit.MILLISECONDS);
				}
			}
			
			
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String messageType = this.message.trim().split("\\s+")[0];
			switch (messageType)
			{
			case "PUTCHUNK":
				putchunk();
				break;
			case "STORED":
				stored();
				break;
			
			default:
					
			}
		}
}
