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

	public GetchunkThread(String[] msg, InetAddress InetAddress) {
		this.messageArray = msg;
		this.chunkId = messageArray[3] + "-" + messageArray[4];
		this.InetAddress = InetAddress;
	}

	@Override
	public void run() {

		if (!Peer.getMemory().savedChunks.containsKey(chunkId)) {
			System.out.println("This peer doesn't contain this chunk: " + chunkId);
		} else {
			byte[] chunkData = Peer.getMemory().savedChunks.get(chunkId).getData();

			String restoredChunk = "CHUNK " + messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
					+ messageArray[4] + "\r\n\r\n";
			System.out.println("SENT " + restoredChunk);
			byte[] data = restoredChunk.getBytes();
			byte[] message = new byte[data.length + chunkData.length];
			System.arraycopy(data, 0, message, 0, data.length);
			System.arraycopy(chunkData, 0, message, data.length, chunkData.length);
			String channel = "mdr";
			Random random = new Random();
			int delay = random.nextInt(401);
			int senderId = Integer.parseInt(messageArray[2]);

			if (Peer.getId() != senderId) {

				if (Peer.getProtocolVersion() == 1.0)
					Peer.getExecutor().schedule(new WorkerThread(message, channel), delay, TimeUnit.MILLISECONDS);
				else{
					TCPSocketPort = Integer.parseInt(messageArray[5]);
					sendByTCP(message);
				} 
				
			}
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
