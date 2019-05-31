package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import threads.sockets.*;
import threads.RestoreFileThread;
import threads.listeners.PeerThread;
import threads.scheduled.SaveMemoryTask;
import utils.*;

public class Peer implements RMIInterface {

	private static int serverID;
	private static String accessPoint;
	private static int peerPort;
	private static InetAddress peerAddress;
	private static ScheduledThreadPoolExecutor executor;
	private static SSLServerSocket peerServerSocket;
	private static Memory memory;
	private static ArrayList<SSLSocket> servers;
	private static int serverIndex = 0;

	public Peer() throws IOException 
	{
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

		memory = Utils.loadMemory(accessPoint + "/memory");

		for (SSLSocket serverSocket : servers) 
		{
			if (serverSocket == null)
			{
				System.out.println("Couldn't connect to server...\nAborting");
				return;
			}
				
			serverSocket.startHandshake();

			try {

			
				String peerID = "Peer " + Peer.getId() + " " + peerAddress.getHostAddress() + " " + peerPort
						+ " "+memory.availableCapacity +"\n", receivedMessage;
				sendMessageToServer(peerID);
				InputStream istream = serverSocket.getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));



				if ((receivedMessage = receiveRead.readLine()) != null) // receive from server
				{
					System.out.println("Connection status: " + receivedMessage);
				}

			} catch (Exception e) {
				System.out.println("ERROR");
				e.printStackTrace();
			}
		}

		peerServerSocket = createServerSocket();

		if (peerServerSocket == null)
			return;

		executor.execute(new PeerThread(peerServerSocket, executor));
	}

	public static SSLSocket createSocket(InetAddress address, int port) {
		SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket;

		try {
			socket = (SSLSocket) ssf.createSocket(address, port);
		} catch (IOException e) {
			try {
				sendMessageToServer("UNAVAILABLE " + address.getHostAddress() + " " + port);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Failed to create SSLSocket");
			return null;
		}

		socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
			"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", 
			"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"});

		socket.setEnabledProtocols(new String[] { "TLSv1.2" });

		return socket;
	}

	private SSLServerSocket createServerSocket() {
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		SSLServerSocket sSocket;

		try {
			sSocket = (SSLServerSocket) ssf.createServerSocket(Peer.peerPort, 30, Peer.peerAddress);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to create server socket");
			return null;
		}

		sSocket.setNeedClientAuth(true);
		sSocket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
				"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"});
		sSocket.setEnabledProtocols(new String[] { "TLSv1.2" });

		return sSocket;
	}

	public static void main(String args[]) throws InterruptedException, IOException, AlreadyBoundException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.rmi.server.hostname", "localhost");

		if (args.length < 5) {
			System.out.println(
					"Usage: Peer <PEER_ID> <SERVICE_ACCESS_POINT> <PEER_PORT> (<SERVER_IP> <SERVER_PORT>)+");
			return;
		}

		if (!Utils.checkStores("peer", ""))
			return;
	
		validateArgs(args);
	}

	private static void validateArgs(String[] args)
			throws RemoteException, InterruptedException, IOException, AlreadyBoundException {
		Peer.servers = new ArrayList<SSLSocket>();
		
		Peer.peerAddress = InetAddress.getLocalHost();
		Peer.serverID = Integer.parseInt(args[0]);
		Peer.accessPoint = args[1];
		Peer.peerPort = Integer.parseInt(args[2]);
		
		
		for(int i = 3; i < args.length - 1; i += 2)
		{
			InetAddress sa = InetAddress.getByName(args[i]);
			int sp = Integer.parseInt(args[i + 1]);

			Peer.servers.add(createSocket(sa, sp));
		}

		Peer peer = new Peer();
		
		RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(peer, 0);

		Registry registry = null;

		try {
			registry = LocateRegistry.getRegistry();
			registry.rebind(accessPoint, stub);
		} catch (RemoteException e) {

			try {
				registry = LocateRegistry.createRegistry(1099);
				registry.rebind(accessPoint, stub);

			} catch (RemoteException e3) {
				e3.printStackTrace();
			}
		}

		SaveMemoryTask saveMemory = new SaveMemoryTask(accessPoint + "/memory", "peer");
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(100);
		executor.scheduleAtFixedRate(saveMemory, 1, 10, TimeUnit.SECONDS);
	}

	// protocols
	@Override
	public void backup(String filename, int repDegree) throws RemoteException, InterruptedException {
		
		FileInfo fileInfo = new FileInfo(filename, repDegree);
		ArrayList<Chunk> chunks = fileInfo.getChunks();
		String chunkId;
		if (memory.hasFileByID(fileInfo.getFileId())){
			System.out.println("This file has already backed up!");
			return;
		}
			
		try {
			for (int i = 0; i < chunks.size(); i++) {
				byte[] header = Utils.getHeader("PUTCHUNK", serverID, fileInfo.getFileId(), chunks.get(i).getChunkNo(),
						repDegree);

				chunkId = fileInfo.getFileId() + "-" + chunks.get(i).getChunkNo();

				if (!memory.savedOcurrences.containsKey(chunkId)) {
					memory.savedOcurrences.put(chunkId, 0);
				}

				byte[] body = chunks.get(i).getData(), message = new byte[header.length + body.length];

				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(body, 0, message, header.length, body.length);

				String backupMessage = "BACKUP " + chunkId + " " + serverID + " " + repDegree + " "+chunks.get(i).getChunkSize()+"\n", receiveMessage;
				try {
					sendMessageToServer(backupMessage);
					System.out.println(backupMessage);
					
				} catch (IOException e) {
					System.out.println("before change server");
					changeServer();
					backup(filename, repDegree);
					return;
				}

				InputStream istream = getServerSocket().getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

				

				if ((receiveMessage = receiveRead.readLine()) != null) {
					String[] splitMessage = receiveMessage.split(" ");

					

					if (receiveMessage.equals(" ")) {
						System.out.println("No peers available");
						continue;
					} else {
						System.out.println("Peers to connect " + receiveMessage);

					}

					if (splitMessage.length < repDegree)
						System.out.println("Warning: There aren't enough peers to meet replication demand");

					for (int j = 0; j < splitMessage.length; j++) {
						String[] split = splitMessage[j].split("-");
						int port = Integer.parseInt(split[1]);
						InetAddress address = InetAddress.getByName(split[0]);
						SSLSocket peerSocket = null;
						peerSocket = createSocket(address, port);

						System.out.println(port + " " + address);
						peerSocket.startHandshake();
						executor.execute(new SenderSocket(peerSocket, message));
						executor.execute(new ReceiverSocket(peerSocket, message, executor));

					}

				}
			}
		} catch (Exception e) {
			System.out.println("Backup Failed");
			return;
		}
		String savedMessage = "SAVED "+serverID+" "+fileInfo.getFileId();
		try {
			sendMessageToServer(savedMessage);
		} catch (IOException e) {
			changeServer();
			backup(filename, repDegree);
			return;
		}

		memory.files.add(fileInfo);
	}

	@Override
	public void restore(String filename) throws RemoteException {
		Path file = Paths.get(filename);
		String fileId = Utils.createFileId(file);
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		FileInfo fileInfo = null;
		String header = null;
		SSLSocket peerSocket = null;

		if (!memory.hasFileByID(fileId)) {
			System.out.println(filename + " has never backed up!");
			return;
		} else {
			try {
				for (int i = 0; i < memory.files.size(); i++) {
					if (memory.files.get(i).getFileId().equals(fileId)) {
						fileInfo = memory.files.get(i);
						chunks = memory.files.get(i).getChunks();
						break;
					}
				}
				for (int i = 0; i < chunks.size(); i++) {
					header = "GETCHUNK " + " " + serverID + " " + fileInfo.getFileId() + " "
							+ chunks.get(i).getChunkNo() + " " + "\r\n\r\n";

					System.out.println("SENT: " + header);

					byte[] message = header.getBytes();
					String restoreMessage = "RESTORE " + fileInfo.getFileId() + " " + serverID + "\n";
					try {
						sendMessageToServer(restoreMessage);
					System.out.println(restoreMessage);
					} catch (IOException e) {
						changeServer();
						restore(filename);
						return;
					}
					InputStream istream = getServerSocket().getInputStream();
					BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));


					String receiveMessage;
					System.out.println(restoreMessage);

					if ((receiveMessage = receiveRead.readLine()) != null) {

						System.out.println("RECEIVED FROM SERVER: " + receiveMessage);
						String[] splitMessage = receiveMessage.split(" ");

						
						if(receiveMessage.equals(" "))
						{
							System.out.println("There weren't any peers with that chunk...\nAborting\n");
							receiveRead.close();
							return;
						} else {
							System.out.println("Peers to connect " + receiveMessage);


						}

						for (int j = 0; j < splitMessage.length; j++) {
							String[] split = splitMessage[j].split("-");
							int port = Integer.parseInt(split[1]);
							InetAddress address = InetAddress.getByName(split[0]);
							peerSocket = createSocket(address, port);
							System.out.println(port + " " + address);
							peerSocket.startHandshake();
							executor.execute(new SenderSocket(peerSocket, message));
							executor.execute(new ReceiverSocket(peerSocket, message, executor));

						}
					}
				}

				int delay = 2000;
				Peer.getExecutor().schedule(
					new RestoreFileThread(filename, fileInfo.getFileId(), chunks.size()), delay, TimeUnit.MILLISECONDS);
			}

			catch (Exception e) {
				System.out.println("Restore Failed");
			}
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {

		FileInfo fileInfo = new FileInfo(filename, 0);
		
		if (!memory.hasFileByID(fileInfo.getFileId())) {
			System.out.println(filename + " has never backed up!");
			return;
		} else {
			memory.removeFile(fileInfo.getFileId());
		}

		try {
			String deleteMessage = "DELETE " + fileInfo.getFileId() + " " + serverID + "\n";
			try {
				sendMessageToServer(deleteMessage);
			} catch (IOException e) {
				changeServer();
				delete(filename);
				return;
			}
			InputStream istream = getServerSocket().getInputStream();
			BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
			String receiveMessage;

			String header = "DELETE " + serverID + " " + fileInfo.getFileId() + " " + "\r\n\r\n";
			System.out.println("SENT: " + header);
			byte[] data;
			data = header.getBytes("US-ASCII");
			byte[] message = new byte[data.length];
			System.arraycopy(data, 0, message, 0, data.length);

			if ((receiveMessage = receiveRead.readLine()) != null) {
				
				String[] splitMessage = receiveMessage.split(" ");

				
				if(receiveMessage.equals(" "))
						{
							System.out.println("There weren't any peers with that chunk...\nAborting\n");
							receiveRead.close();
							return;
				}
				else {
					System.out.println("Peers to connect " + receiveMessage);
				}
				for (int j = 0; j < splitMessage.length; j++) {

					String[] split = splitMessage[j].split("-");
					int port = Integer.parseInt(split[1]);
					SSLSocket peerSocket = null;

					InetAddress address = InetAddress.getByName(split[0]);
					peerSocket = createSocket(address, port);

					System.out.println(port + " " + address);

					peerSocket.startHandshake();

					executor.execute(new SenderSocket(peerSocket, message));
					executor.execute(new ReceiverSocket(peerSocket, message, executor));
				}

			}

		} catch (Exception e) {

		}
	}

	@Override
	public void reclaim(int space) throws RemoteException {

		int currentSpaceToFree = memory.getUsedMemory() - space; // space to free

		if (currentSpaceToFree > 0) {

			List<String> sortedChunks = sortChunksToDelete();

			for (Iterator<String> iterator = sortedChunks.iterator(); iterator.hasNext();) {
				String[] splitString = iterator.next().trim().split(":");
				String key = splitString[0];

				if (currentSpaceToFree > 0) {
					currentSpaceToFree -= memory.savedChunks.get(key).getChunkSize();
					String header = "REMOVED " + serverID + " " + memory.savedChunks.get(key).getFileId() + " "
							+ memory.savedChunks.get(key).getChunkNo() + " "
							+ memory.savedChunks.get(key).getReplicationDegree() + " "+space+ "\r\n\r\n";
					System.out.print(header);

					
					try {
						sendMessageToServer(header);
					} catch (IOException e) {
						changeServer();
						reclaim(space);
						return;
					}
				
					InputStream istream = null;
					try {
						istream = getServerSocket().getInputStream();
						BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
						String receiveMessage;
						
						if ((receiveMessage = receiveRead.readLine()) != null) {
							int repDegree = Integer.parseInt(receiveMessage);
							if (repDegree > 0) {
								backupChunk(memory.savedChunks.get(key).getFileId(),memory.savedChunks.get(key).getChunkNo(),repDegree,memory.savedChunks.get(key).getData());	
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					String[] splitKey = key.trim().split("-");
					String filePath = "Peer" + Peer.getId() + "/" + "STORED" + "/" + splitKey[0] + "/" + splitKey[1]
							+ "-" + memory.savedChunks.get(key).getReplicationDegree();
					File fileToDelete = new File(filePath);
					fileToDelete.delete();
					iterator.remove();
					Peer.getMemory().savedChunks.remove(key);
				}
	
			}
		}
		else
		{
			try
			{
				sendMessageToServer("MEMORY " + serverID + " " + space);
			}
			catch(IOException e)
			{
				System.out.println("Couldn't send updated memory to server");
			}
			
		}

		Peer.getMemory().capacity = space;
		Peer.getMemory().memoryUsed = Peer.getMemory().getUsedMemory();
		System.out.println("Memory used: " + Peer.getMemory().memoryUsed + " of " + Peer.getMemory().capacity);

	}

	public void backupChunk(String fileId,int chunkNo,int repDegree,byte[] body){
		try{
		byte[] header = Utils.getHeader("PUTCHUNK", serverID, fileId, chunkNo,
						repDegree);

				String chunkId = fileId +"-"+chunkNo;

				
				byte[] message = new byte[header.length + body.length];

				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(body, 0, message, header.length, body.length);

				String backupMessage = "BACKUP " + chunkId + " " + serverID + " " + repDegree + " "+body.length+"\n", receiveMessage;

				try {
				sendMessageToServer(backupMessage);
				} catch (IOException e) {
					changeServer();
					backupChunk(fileId,chunkNo,repDegree,body);
					return;
				}

				InputStream istream = getServerSocket().getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
			

				System.out.println(backupMessage);

				if ((receiveMessage = receiveRead.readLine()) != null) {
					String[] splitMessage = receiveMessage.split(" ");

					

					if (receiveMessage.equals(" ")) {
						System.out.println("No peers available");
						return;
					} else {
						System.out.println("Peers to connect " + receiveMessage);

					}

					if (splitMessage.length < repDegree)
						System.out.println("Warning: There aren't enough peers to meet replication demand");

					for (int j = 0; j < splitMessage.length; j++) {
						String[] split = splitMessage[j].split("-");
						int port = Integer.parseInt(split[1]);
						InetAddress address = InetAddress.getByName(split[0]);
						SSLSocket peerSocket = null;

						peerSocket = createSocket(address, port);
						

						System.out.println(port + " " + address);
						peerSocket.startHandshake();
						executor.execute(new SenderSocket(peerSocket, message));
						executor.execute(new ReceiverSocket(peerSocket, message, executor));
					}
				}
		} catch (Exception e) {
			System.out.println("Backup Failed");

		}

	}


	@Override
	public void state() throws RemoteException {

		int i;
		// Backup
		System.out.println("\nPEER " + Peer.getId() + " STATE");
		System.out.println("\nFor each file whose backup it has initiated:");
		for (i = 0; i < memory.files.size(); i++) {
			System.out.println("-File path: " + memory.files.get(i).getFilePath());
			System.out.println("-Backup service id of the file:" + memory.files.get(i).getFileId());
			System.out.println("-Replication degree:" + memory.files.get(i).getReplicationDegree());
			for (int j = 0; j < memory.files.get(i).getChunks().size(); j++) {
				System.out.println("\n Backup chunks\n");
				System.out.println("--Chunk id: " + memory.files.get(i).getChunks().get(j).getChunkId());
				System.out.println("--Perceived replication degree: "
						+ memory.savedOcurrences.get(memory.files.get(i).getChunks().get(j).getChunkId()));

			}
		}
		// Stored chunks
		System.out.println("\n Stored chunks\n");
		for (String key : memory.savedChunks.keySet()) {
			System.out.println("--Chunk id: " + key);
			System.out.println("--Chunk size: " + memory.savedChunks.get(key).getChunkSize());
			System.out.println("--Perceived replication degree: " + memory.savedOcurrences.get(key));

		}

		// Storage capacity
		System.out.println("\nThe maximum amount of disk space that can be used to store chunks: " + memory.capacity);
		System.out.println("The amount of storage used to backup the chunks: " + memory.memoryUsed);

	}
	// gets

	public static Memory getMemory() {
		return memory;
	}

	public static ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	public static int getId() {
		return serverID;
	}

	public static void sendMessageToServer(String msg) throws IOException {
		OutputStream ostream = getServerSocket().getOutputStream();
		PrintWriter pwrite = new PrintWriter(ostream, true);
		pwrite.println(msg);
		pwrite.flush();
	}

	public static SSLSocket getServerSocket() {
		return servers.get(serverIndex);
	}

	public static void changeServer() {
		System.out.print("Changing server from " + serverIndex);

		if (serverIndex == servers.size() - 1)
			serverIndex = 0;
		else
			serverIndex++;

		System.out.println(" to " + serverIndex);

	}

	public static List<String> sortChunksToDelete() {
		ArrayList<String> chunksToSort = new ArrayList<String>();
		for (String key : memory.savedChunks.keySet()) {
			int diff = memory.savedOcurrences.get(key) - memory.savedChunks.get(key).getReplicationDegree();
			String chunk = key + ":" + diff;
			chunksToSort.add(chunk);
		}
		chunksToSort.sort((o1, o2) -> {
			int chunk1 = Integer.valueOf(o1.split(":")[1]);
			int chunk2 = Integer.valueOf(o2.split(":")[1]);
			return Integer.compare(chunk1, chunk2);
		});

		List<String> returnList = chunksToSort;
		Collections.reverse(returnList);
		return returnList;
	}


}