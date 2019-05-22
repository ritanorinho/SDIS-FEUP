package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import threads.sockets.*;
import threads.listeners.PeerThread;
import utils.*;

public class Peer implements RMIInterface {

	private static int serverID;
	private static String accessPoint;
	private static int peerPort;
	private static InetAddress peerAddress;
	private static ScheduledThreadPoolExecutor executor;
	private static SSLServerSocket peerServerSocket;
	private static Memory memory = new Memory();
	private static ArrayList<SSLSocket> servers;
	private static int serverIndex = 0;

	public Peer() throws IOException 
	{
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

		for (SSLSocket serverSocket : servers) 
		{
			if (serverSocket == null)
			{
				System.out.println("Couldn't connect to server...\nAborting");
				return;
			}
				
			serverSocket.startHandshake();

			try {

				OutputStream ostream = serverSocket.getOutputStream();
				PrintWriter pwrite = new PrintWriter(ostream, true);
				String peerID = "Peer " + Peer.getId() + " " + Peer.peerAddress.getHostAddress() + " " +  Peer.peerPort + "\n", receivedMessage;
				InputStream istream = serverSocket.getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

				pwrite.println(peerID);
				pwrite.flush();

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
			System.out.println("Failed to create SSLSocket");
			return null;
		}

		socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
				"SSL_RSA_WITH_NULL_MD5", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA" });

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
				"SSL_RSA_WITH_NULL_MD5", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA" });
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

		if (!checkStores())
			return;

		validateArgs(args);
	}

	public static boolean checkStores() {
		File store = new File("keystore.jks");

		if (!store.exists()) {
			System.out.println("Couldn't find peer key store");

			return false;
		}

		store = new File("truststore.jks");

		if (!store.exists()) {
			System.out.println("Couldn't find peer trust store");

			return false;
		}

		System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

		return true;
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
	}

	// protocols
	@Override
	public void backup(String filename, int repDegree) throws RemoteException, InterruptedException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file, filename, repDegree);
		ArrayList<Chunk> chunks = fileInfo.getChunks();
		String chunkId;

		try {
			for (int i = 0; i < chunks.size(); i++) {
				byte[] header = Utils.getHeader("PUTCHUNK", serverID, fileInfo.getFileId(), chunks.get(i).getChunkNo(),
						repDegree);

				chunkId = fileInfo.getFileId() + "-" + chunks.get(i).getChunkNo();

				if (!memory.hasFileByID(fileInfo.getFileId()))
					memory.files.add(fileInfo);

				if (!memory.savedOcurrences.containsKey(chunkId)) {
					memory.savedOcurrences.put(chunkId, 0);
					Utils.savedOccurrencesFile();
				}

				byte[] body = chunks.get(i).getData(), message = new byte[header.length + body.length];

				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(body, 0, message, header.length, body.length);

				OutputStream ostream = null;
				try {
					ostream = getServerSocket().getOutputStream();
				} catch (IOException e) {
					changeServer();
					backup(filename, repDegree);
					return;
				}
				PrintWriter pwrite = new PrintWriter(ostream, true);

				InputStream istream = getServerSocket().getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
				String backupMessage = "BACKUP " + chunkId + " " + serverID + " " + repDegree + "\n", receiveMessage;

				pwrite.println(backupMessage);
				pwrite.flush();

				System.out.println(backupMessage);

				if ((receiveMessage = receiveRead.readLine()) != null) {	
					String[] splitMessage = receiveMessage.split(" ");

					System.out.println("Peers to connect " + receiveMessage);

					if(receiveMessage.equals(" "))
					{
						System.out.println("No peers available");
						continue;
					}

					if(splitMessage.length < repDegree)
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
		}
	}

	@Override
	public void restore(String filename) throws RemoteException {
		File file = new File(filename);
		String fileId = Utils.createFileId(file);
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		FileInfo fileInfo = null;
		String header = null;

		if (!memory.hasFileByID(fileId)) {
			System.out.println(filename + "has never backed up!");
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
					OutputStream ostream = null;
					try{
					 ostream = getServerSocket().getOutputStream();
					}catch(IOException e){
						changeServer();
						restore(filename);
						return;
					}
					PrintWriter pwrite = new PrintWriter(ostream, true);
					InputStream istream = getServerSocket().getInputStream();
					BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

					String restoreMessage = "RESTORE " + fileInfo.getFileId() + " Peer" + serverID + "\n";
					pwrite.println(restoreMessage);
					pwrite.flush();

					String receiveMessage;
					System.out.println(restoreMessage);

					if ((receiveMessage = receiveRead.readLine()) != null) {
						System.out.println("RECEIVED FROM SERVER: " + receiveMessage);
						String[] splitMessage = receiveMessage.split(" ");

						System.out.println("Peers to connect " + receiveMessage);

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
				System.out.println("Restore Failed");
			}
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file, filename, 0);

		if (!memory.hasFileByName(file.getName())) {
			System.out.println(filename + " has never backed up!");
			return;
		}

		try {
			String deleteMessage = "DELETE " + fileInfo.getFileId() + " Peer" + serverID + "\n";

			OutputStream ostream = null;
			try{
			ostream = getServerSocket().getOutputStream();
			}catch(IOException e){
				changeServer();
				delete(filename);
				return;
			}
			PrintWriter pwrite = new PrintWriter(ostream, true);

			InputStream istream = getServerSocket().getInputStream();
			BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
			pwrite.println(deleteMessage);
			pwrite.flush();
			String receiveMessage;

			String header = "DELETE " + serverID + " " + fileInfo.getFileId() + " " + "\r\n\r\n";
			System.out.println("SENT: " + header);
			byte[] data;
			data = header.getBytes("US-ASCII");
			byte[] message = new byte[data.length];
			System.arraycopy(data, 0, message, 0, data.length);

			if ((receiveMessage = receiveRead.readLine()) != null) {
				String[] splitMessage = receiveMessage.split(" ");

				System.out.println("Peers to connect " + receiveMessage);

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

}