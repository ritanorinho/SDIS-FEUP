package project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.OutputStream;
import java.io.PrintWriter;

import threads.*;
import utils.Chunk;
import utils.FileInfo;
import utils.Memory;
import utils.Utils;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import sockets.ReceiverSocket;
import sockets.SenderSocket;

public class Peer implements RMIInterface {

	private static double protocolVersion;
	private static int serverID;
	private static String accessPoint;
	private int serverPort;
	private InetAddress serverAddress;
	private int peerPort;
	private InetAddress peerAddress;
	private static ScheduledThreadPoolExecutor executor;
	private static SSLSocket serverSocket;
	private static SSLServerSocket peerServerSocket;
	private static Memory memory = new Memory();

	public Peer(Integer serverPort, InetAddress serverAddress, Integer peerPort, InetAddress peerAddress)
			throws IOException {
		this.serverPort = serverPort;
		this.serverAddress = serverAddress;
		this.peerPort = peerPort;
		this.peerAddress = peerAddress;

		/*
		 * String[] supportedSuites = socket.getSupportedCipherSuites();
		 * 
		 * for(int i = 0; i < supportedSuites.length; i++)
		 * System.out.println(supportedSuites[i]);
		 */

		if (!createStores())
			return;

		serverSocket = createSocket();

		if(serverSocket == null)
		{
			System.out.println("Couldn't connect to server");
			return;
		}

		serverSocket.startHandshake();

		System.out.println("Connection to the server estabelished");

		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

		try {
			
			OutputStream ostream = serverSocket.getOutputStream();
			PrintWriter pwrite = new PrintWriter(ostream, true);
			String peerID = "Peer" + Peer.getId() + " " + peerPort + " " + peerAddress + "\n";
			pwrite.println(peerID);
			pwrite.flush();
			InputStream istream = serverSocket.getInputStream();
			BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
			System.out.println(peerID);
			String receiveMessage;
			if ((receiveMessage = receiveRead.readLine()) != null) // receive from server
			{
				System.out.println("conection " + receiveMessage);
			}

		} catch (Exception e) {
			System.out.println("ERROR");
			e.printStackTrace();
		}
		createPeerSocket();
	}

	public SSLSocket createSocket()
	{
		SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket;

		try {
			socket = (SSLSocket) ssf.createSocket(serverAddress, serverPort);
		} catch (IOException e) {
			System.out.println("Peer - Failed to create SSLSocket");
			e.getMessage();
			return null;
		}

		socket.setEnabledCipherSuites(new String[] 
		{
			"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_NULL_MD5",
			"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
			"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA"
		});

		socket.setEnabledProtocols(new String[] {"TLSv1.2"});

		return socket;
	}

	private void createPeerSocket() {

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			System.out.println("port " + this.peerPort);
			peerServerSocket = (SSLServerSocket) ssf.createServerSocket(this.peerPort);
			System.out.println("port " + peerServerSocket.getLocalPort());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server - Failed to create SSLServerSocket");
			return;
		}
		peerServerSocket.setNeedClientAuth(false); // TODO Change
		peerServerSocket.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" });
		executor.execute(new PeerThread(peerServerSocket, executor));
	}

	public static void main(String args[]) throws InterruptedException, IOException, AlreadyBoundException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.rmi.server.hostname", "localhost");
		if (args.length != 7) {
			System.out.println(
					"ERROR: Peer format : Peer <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT> <TCP_IP> <TCP_PORT>");
			return;
		}

		validateArgs(args);
		loadMemory();
		loadOccurrences();
		if (protocolVersion == 1.1) // delete enhancement
			alive();
	}

	public static boolean createStores() {
		char[] pwdArray = "password".toCharArray();
		KeyStore ks, ts;
		String peerFolder = "Peer" + Peer.getId();

		new File(peerFolder).mkdirs();

		try {
			ts = KeyStore.getInstance("JKS");
			ts.load(new FileInputStream("truststore.jks"), pwdArray);
		} catch (Exception e) {
			try {
				ts = KeyStore.getInstance(KeyStore.getDefaultType());
				ts.load(null, pwdArray);

				try (FileOutputStream fos = new FileOutputStream("truststore.jks")) {
					ts.store(fos, pwdArray);
				}
			} catch (Exception e2) {
				System.out.println("Couldn't create trust store");
				e2.printStackTrace();
				return false;
			}
		}

		System.setProperty("javax.net.ssl.trustStore", peerFolder + "/truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

		return true;
	}

	private static void validateArgs(String[] args)
			throws RemoteException, InterruptedException, IOException, AlreadyBoundException {

		InetAddress serverAddress = InetAddress.getByName(args[3]);
		Integer serverPort = Integer.parseInt(args[4]);
		InetAddress peerAddress = InetAddress.getByName(args[5]);
		Integer peerPort = Integer.parseInt(args[6]);
		protocolVersion = Double.parseDouble(args[0]);
		serverID = Integer.parseInt(args[1]);
		accessPoint = args[2];

		Peer peer = new Peer(serverPort, serverAddress, peerPort, peerAddress);
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
	public void backup(String filename, int repDegree, boolean enhancement)
			throws RemoteException, InterruptedException {
		System.out.println(Server.getMemory().conections);
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file, filename, repDegree);
		double workingVersion = getWorkingVersion(enhancement);
		ArrayList<Chunk> chunks = fileInfo.getChunks();
		String chunkId;
		try {
			for (int i = 0; i < chunks.size(); i++) {

				byte[] header = Utils.getHeader("PUTCHUNK", workingVersion, serverID, fileInfo.getFileId(),
						chunks.get(i).getChunkNo(), repDegree);
				String headerString = new String(header, 0, header.length);

				System.out.println("SENT: " + headerString);

				chunkId = fileInfo.getFileId() + "-" + chunks.get(i).getChunkNo();

				if (!memory.hasFileByID(fileInfo.getFileId()))
					memory.files.add(fileInfo);

				if (!memory.savedOcurrences.containsKey(chunkId)) {
					memory.savedOcurrences.put(chunkId, 0);
					Utils.savedOccurrencesFile();
				}

				byte[] body = chunks.get(i).getData();
				byte[] message = new byte[header.length + body.length];
				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(body, 0, message, header.length, body.length);
				OutputStream ostream = serverSocket.getOutputStream();
				PrintWriter pwrite = new PrintWriter(ostream, true);

				InputStream istream = serverSocket.getInputStream();

				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

				String backupMessage = "BACKUP " + fileInfo.getFileId() + " Peer" + serverID + "\n";
				pwrite.println(backupMessage);
				pwrite.flush();
				String receiveMessage;
				System.out.println(backupMessage);

				if ((receiveMessage = receiveRead.readLine()) != null) {
					System.out.println(receiveMessage);
					String[] splitMessage = receiveMessage.split("-");
					int port = Integer.parseInt(splitMessage[0]);
					InetAddress address = InetAddress.getByName("localhost");
					System.out.println(port + " " + address);
					SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
					SSLSocket peerSocket;
					try {
						peerSocket = (SSLSocket) ssf.createSocket(address, port);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Peer - Failed to create SSLSocket");
						return;
					}
					peerSocket.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" });
					peerSocket.startHandshake();

					executor.execute(new SenderSocket(peerSocket,message));
					executor.execute(new ReceiverSocket(peerSocket,message,executor));


				}
			}
		} catch (Exception e) {

		}
		if (workingVersion < 0) {
			System.out.println("This version does not support this opperation");
			return;
		}
	}

	@Override
	public void restore(String filename, boolean enhancement) throws RemoteException {
		File file = new File(filename);
		String fileId = Utils.createFileId(file);
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		FileInfo fileInfo = null;
		String header = null;
		double workingVersion = getWorkingVersion(enhancement);

		if (workingVersion < 0) {
			System.out.println("This version does not support this opperation");
			return;
		}

		if (!memory.hasFileByID(fileId)) {
			System.out.println(filename + "has never backed up!");
			return;
		} else {

			for (int i = 0; i < memory.files.size(); i++) {
				if (memory.files.get(i).getFileId().equals(fileId)) {
					fileInfo = memory.files.get(i);
					chunks = memory.files.get(i).getChunks();
					break;
				}
			}
			for (int i = 0; i < chunks.size(); i++) {
				header = "GETCHUNK " + workingVersion + " " + serverID + " " + fileInfo.getFileId() + " "
						+ chunks.get(i).getChunkNo() + " " + "\r\n\r\n";

				byte[] message = header.getBytes();

				System.out.println("SENT: " + header);
				String channel = "mc";
				Peer.executor.execute(new WorkerThread(message, channel));
			}
			Peer.executor.schedule(
					new RestoreFileThread(fileInfo.getFilename(), fileInfo.getFileId(), chunks.size(), workingVersion),
					10, TimeUnit.SECONDS);
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file, filename, 0);

		if (!memory.hasFileByName(file.getName())) {
			System.out.println(filename + "has never backed up!");
			return;
		}

		String header = "DELETE " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + " " + "\r\n\r\n";
		System.out.println("SENT: " + header);

		byte[] data;
		try {
			data = header.getBytes("US-ASCII");
			byte[] message = new byte[data.length];
			System.arraycopy(data, 0, message, 0, data.length);
			String channel = "mc";
			Peer.executor.execute(new WorkerThread(data, channel));

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
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
					String header = "REMOVED " + protocolVersion + " " + serverID + " "
							+ memory.savedChunks.get(key).getFileId() + " " + memory.savedChunks.get(key).getChunkNo()
							+ " " + "\r\n\r\n";
					System.out.print(header);

					try {
						byte[] data = header.getBytes("US-ASCII");
						String channel = "mc";
						executor.execute(new WorkerThread(data, channel));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					String[] splitKey = key.trim().split("-");
					String filePath = "Peer" + Peer.getId() + "/" + "STORED" + "/" + splitKey[0] + "/" + splitKey[1]
							+ "-" + memory.savedChunks.get(key).getReplicationDegree();
					File fileToDelete = new File(filePath);
					fileToDelete.delete();
					iterator.remove();
					Peer.getMemory().savedOcurrences.put(key, Peer.getMemory().savedOcurrences.get(key) - 1);
					Utils.savedOccurrencesFile();
					Peer.getMemory().savedChunks.remove(key);
				}

			}
		}

		Peer.getMemory().capacity = space;
		Peer.getMemory().memoryUsed = Peer.getMemory().getUsedMemory();
		System.out.println("Memory used: " + Peer.getMemory().memoryUsed + " of " + Peer.getMemory().capacity);

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

	public static void alive() {

		String aliveMessage = "ALIVE " + protocolVersion + " " + serverID + " " + "\r\n\r\n";
		System.out.println("\nSENT: " + aliveMessage);
		try {
			byte[] byteMessage = aliveMessage.getBytes("US-ASCII");
			String channel = "mc";
			executor.execute(new WorkerThread(byteMessage, channel));

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	// gets

	public static double getProtocolVersion() {
		return protocolVersion;
	}

	public static Memory getMemory() {
		return memory;
	}

	public static ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	public static int getId() {
		return serverID;
	}

	public double getWorkingVersion(boolean enhancement) {
		double ret;

		if (enhancement && protocolVersion == 1.0)
			ret = -1;
		else if (!enhancement && protocolVersion == 1.1)
			ret = 1.0;
		else
			ret = protocolVersion;

		return ret;
	}

	public static void loadMemory() {
		System.out.println();
		String storedDirectory = "Peer" + Peer.getId() + "/STORED/";
		File storedFile = new File(storedDirectory);
		if (!storedFile.exists()) {
			System.out.println("Peer " + Peer.getId() + " has no data in memory.");
			return;
		} else {

			String[] allFiles;
			if (storedFile.isDirectory()) {
				allFiles = storedFile.list();
				for (int i = 0; i < allFiles.length; i++) {
					String fileDirectory = storedDirectory + allFiles[i];
					File file = new File(fileDirectory);
					if (file.isDirectory()) {
						String[] allChunks = file.list();
						for (int j = 0; j < allChunks.length; j++) {
							String chunkDirectory = fileDirectory + "/" + allChunks[j];
							String[] splitChunkId = allChunks[j].trim().split("-");
							int chunkNo = Integer.parseInt(splitChunkId[0]);
							int replicationDegree = Integer.parseInt(splitChunkId[1]);
							File chunkFile = new File(chunkDirectory);
							byte[] content = new byte[(int) chunkFile.length()];
							FileInputStream in;
							try {
								in = new FileInputStream(chunkFile);
								in.read(content);
								in.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							String chunkId = allFiles[i].trim() + "-" + chunkNo;
							Chunk chunk = new Chunk(allFiles[i].trim(), chunkNo, content, (int) chunkFile.length(),
									chunkId.trim(), replicationDegree);
							if (!memory.savedChunks.containsKey(chunkId))
								memory.savedChunks.put(chunkId, chunk);
						}
					}

				}
			}
		}
	}

	public static void loadOccurrences() {
		String storedDirectory = "Peer" + Peer.getId() + "/SAVED/savedOccurrences.txt";
		File storedFile = new File(storedDirectory);
		if (!storedFile.exists()) {
			System.out.println("Peer " + Peer.getId() + " has no data in memory.");
		} else {
			FileInputStream in;
			try {
				in = new FileInputStream(storedFile);
				BufferedReader buf = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String line;

				while ((line = buf.readLine()) != null) {
					String[] splitLine = line.trim().split(" ");
					String chunkId = splitLine[0].trim();
					int occurrences = Integer.parseInt(splitLine[1]);
					if (!memory.savedOcurrences.containsKey(chunkId))
						memory.savedOcurrences.put(chunkId, occurrences);
				}
				buf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
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