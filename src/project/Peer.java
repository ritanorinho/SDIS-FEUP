package project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
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

import listeners.MCListener;
import listeners.MDBListener;
import listeners.MDRListener;
import threads.*;
import utils.Chunk;
import utils.FileInfo;
import utils.Memory;
import utils.Utils;

public class Peer implements RMIInterface {

	private static double protocolVersion = 1.0;
	private static int serverID;
	private static String accessPoint;
	private static volatile MCListener mcListener;
	private static volatile MDBListener mdbListener;
	private static volatile MDRListener mdrListener;
	private static ScheduledThreadPoolExecutor executor;
	private static Memory memory = new Memory();

	public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws IOException {
		mcListener = new MCListener(mcAddress, mcPort);
		mdbListener = new MDBListener(mdbAddress, mdbPort);
		mdrListener = new MDRListener(mdrAddress, mdrPort);
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

	}

	public static void main(String args[]) throws InterruptedException, IOException, AlreadyBoundException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.rmi.server.hostname", "localhost");

		if (args.length != 9) {
			System.out.println(
					"ERROR: Peer format : Peer <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT> <MC_IP> <MC_Port> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> ");
			return;
		}

		validateArgs(args);

		executor.execute(mcListener);
		executor.execute(mdbListener);
		executor.execute(mdrListener);
		loadMemory();
		loadOccurrences();
		if (protocolVersion == 1.1) //delete enhancement
			alive();
	}

	private static void validateArgs(String[] args)
			throws RemoteException, InterruptedException, IOException, AlreadyBoundException {

		InetAddress MCAddress = InetAddress.getByName(args[3]);
		Integer MCPort = Integer.parseInt(args[4]);
		InetAddress MDBAddress = InetAddress.getByName(args[5]);
		Integer MDBPort = Integer.parseInt(args[6]);
		InetAddress MDRAddress = InetAddress.getByName(args[7]);
		Integer MDRPort = Integer.parseInt(args[8]);
		protocolVersion = Double.parseDouble(args[0]);
		serverID = Integer.parseInt(args[1]);
		accessPoint = args[2];

		Peer peer = new Peer(MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
		RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(peer, 0);

		Registry registry;

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

		for (int i = 0; i < chunks.size(); i++) {

			byte[] header = Utils.getHeader("PUTCHUNK", protocolVersion, serverID, fileInfo.getFileId(),
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

			String channel = "mdb";
			Peer.executor.execute(new WorkerThread(message, channel));

			// The initiator-peer collects the confirmation
			// messages during a time interval of one second
			Peer.executor.schedule(new BackupThread(chunkId, message, repDegree), 1, TimeUnit.SECONDS);

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

			for (int i = 0; i < memory.files.size(); i++) {
				if (memory.files.get(i).getFileId().equals(fileId)) {
					fileInfo = memory.files.get(i);
					chunks = memory.files.get(i).getChunks();
					break;
				}
			}
			for (int i = 0; i < chunks.size(); i++) {
				header = "GETCHUNK " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + " "
						+ chunks.get(i).getChunkNo() + " ";
				header += "\r\n\r\n";

				byte[] message = header.getBytes();

				System.out.println("SENT: " + header);
				String channel = "mc";
				Peer.executor.execute(new WorkerThread(message, channel));
			}
			Peer.executor.schedule(new RestoreFileThread(fileInfo.getFilename(), fileInfo.getFileId(), chunks.size()),
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

			// The initiator-peer collects the confirmation
			// messages during a time interval of one second
			Peer.getMCListener().message(message);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch(IOException e2){
			e2.printStackTrace();
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
					String header = "REMOVED " + protocolVersion+" "+serverID + " " + memory.savedChunks.get(key).getFileId() + " "
							+ memory.savedChunks.get(key).getChunkNo() + " " + "\r\n\r\n";
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
					Peer.getMemory().savedOcurrences.put(key,Peer.getMemory().savedOcurrences.get(key)-1);
					Utils.savedOccurrencesFile();
					Peer.getMemory().savedChunks.remove(key);
				}

				Peer.getMemory().capacity = space;
				Peer.getMemory().memoryUsed = Peer.getMemory().getUsedMemory();
				System.out.println("CAPACITY " + Peer.getMemory().capacity + " " + Peer.getMemory().memoryUsed);
			}
		} else {
			System.out.println("\nPeer " + Peer.getId() + " doesn't have space to free!");
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
		
		//Storage capacity
		System.out.println("\nThe maximum amount of disk space that can be used to store chunks: "+memory.capacity);
		System.out.println("The amount of storage used to backup the chunks: "+memory.memoryUsed);
		
	}
	public static void alive() {
		
		String aliveMessage = "ALIVE "+protocolVersion + " " + serverID+" "+"\r\n\r\n";
		System.out.println("\nSENT: "+aliveMessage);
		try {
			byte[] byteMessage = aliveMessage.getBytes("US-ASCII");
			String channel ="mc";
			executor.execute(new WorkerThread(byteMessage,channel));
			
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

	public static MDBListener getMDBListener() {
		return mdbListener;
	}

	public static ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	public static int getId() {
		return serverID;
	}

	public static MCListener getMCListener() {
		return mcListener;
	}

	public static MDRListener getMDRListener() {
		return mdrListener;
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
							if(!memory.savedChunks.containsKey(chunkId))
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
					 int occurrences= Integer.parseInt(splitLine[1]);
					 if(!memory.savedOcurrences.containsKey(chunkId))
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