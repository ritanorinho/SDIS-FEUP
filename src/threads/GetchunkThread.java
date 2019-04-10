package threads;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import java.io.OutputStream;

import project.Peer;

public class GetchunkThread implements Runnable {
	private String[] messageArray;
	private String chunkId;
	private InetAddress InetAddress;
	private Socket socket;
	private int TCPSocketPort;

	public GetchunkThread(String[] msg) {
		this.messageArray = msg;
		this.chunkId = messageArray[3] + "-" + messageArray[4];
	}


	@Override
	public void run() {

		if (!Peer.getMemory().savedChunks.containsKey(chunkId)) {
			System.out.println("This peer doesn't contain this chunk: "+chunkId);
			return;
		}
		
		if(Peer.getProtocolVersion()==2.0)
			sendChunkMulticast();
		
		else{
			int senderId = Integer.parseInt(messageArray[2]);
			if (Peer.getId() != senderId) 
				confirmChunk();

				
				(new TCPRestoreServer(Peer.getTCPPort(), chunkId)).start();
		}
	}

	public void confirmChunk(){
		try {
			String storedMessage = "CONFIRMCHUNK "+Peer.getProtocolVersion()+" "+Peer.getId()+" "+ chunkId +" "+Peer.getTCPPort()+"\n\r\n\r";
			System.out.println(storedMessage);
			Peer.getMCListener().message(storedMessage.getBytes("US-ASCII"));

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}


	public void sendChunkMulticast(){
		String channel ="mdr";
		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);

		byte[] chunkData = Peer.getMemory().savedChunks.get(chunkId).getData();
	
		String restoredChunk = "CHUNK " + messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
				+ messageArray[4] + "\r\n\r\n";
		byte[] data = restoredChunk.getBytes();
		byte[] message = new byte[data.length + chunkData.length];

		System.arraycopy(data, 0, message, 0, data.length);
		System.arraycopy(chunkData, 0, message, data.length, chunkData.length);
		
		if (Peer.getId() != senderId ){	
			Peer.getExecutor().schedule(new WorkerThread(message,channel), delay,
					TimeUnit.MILLISECONDS);
		}
	}

	public void sendByTCP(byte[] message) {
		OutputStream os;
		DataOutputStream dos;

		try {
			this.socket = new Socket(InetAddress, TCPSocketPort);

			os = socket.getOutputStream();
			dos = new DataOutputStream(os);
			
			dos.writeInt(message.length);
			dos.write(message, 0, message.length);

			this.socket.close();

			System.out.println("sent by tcp");

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}