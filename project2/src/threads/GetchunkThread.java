package threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


import app.Peer;

public class GetchunkThread implements Runnable {
	private String[] messageArray;
	private String chunkId;
	private double senderVersion;
	private Socket socket;

	public GetchunkThread(String[] msg, Socket socket) {
		this.messageArray = msg;
		this.chunkId = messageArray[3] + "-" + messageArray[4];
		this.senderVersion = Double.parseDouble(messageArray[1]);
		this.socket = socket;
	}

	public String sendConfirmChunk(int port){
		String storedMessage = null;

		/*
		try {
			storedMessage = "CONFIRMCHUNK "+this.senderVersion+" "+Peer.getId()+" "+ chunkId +" "+port+"\r\n\r\n";
			Peer.getMCListener().message(storedMessage.getBytes("US-ASCII"));
			System.out.println(storedMessage);


		} catch (IOException e) {
			e.printStackTrace();
		} */

		return storedMessage;
	}


	public byte[] chunkMessage(String restoredChunk){

		byte[] chunkData = Peer.getMemory().savedChunks.get(chunkId).getData();
	
		restoredChunk = "CHUNK " + messageArray[1] + " " + messageArray[2] + " " + messageArray[3] + " "
				+ messageArray[4] + " " + "\r\n\r\n";
		byte[] data = restoredChunk.getBytes();
		byte[] message = new byte[data.length + chunkData.length];

		System.arraycopy(data, 0, message, 0, data.length);
		System.arraycopy(chunkData, 0, message, data.length, chunkData.length);

		return message;
	}

	public void sendChunkMulticast(byte[] message){
		String channel ="mdr";
		Random random = new Random();
		int delay = random.nextInt(401);
		int senderId = Integer.parseInt(messageArray[2]);	
		if (Peer.getId() != senderId ){	
			
		}
	}

	public void sendChunk(byte[] message){
		System.out.println("\nSENT lalala" + message);
		try{
			OutputStream outputStream = socket.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeInt(message.length);
			dataOutputStream.write(message);
		}
		catch(Exception e){
			System.out.println("Error sending chunk to RESTORE");
		}
	}

	public int attributePort(){
		Random random = new Random();
		int port;

        do{
            port = random.nextInt(1000)+8000;
        }
		while(isPortInUse(port));
		
		return port;
    }

    public boolean isPortInUse(int port){
        boolean result = true;

        try {
            (new ServerSocket(port)).close();
            result = false;
        } catch (IOException e) {}

        return result;
	}
	
	@Override
	public void run() {
		System.out.println("GETCHUNK THREAD WORKING FINE");
		if (!Peer.getMemory().savedChunks.containsKey(chunkId)) {
			System.out.println("This peer doesn't contain this chunk: "+chunkId);
			return;
		}
		
		int senderId = Integer.parseInt(messageArray[2]);

		if (Peer.getId() != senderId) {
			String msg = "";
			byte[] message = chunkMessage(msg);

			if(this.senderVersion==1.0){
				System.out.println("SENDING CHUNK");
				sendChunk(message);
				// sendChunkMulticast(message);
				System.out.println(msg);		
			}else{
				int port = this.attributePort();
				this.sendConfirmChunk(port);
				(new TCPRestoreServer(port, chunkId, message)).start();
			}
		}
	}
}