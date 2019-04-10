package project;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
	private static int TCPSocketPort = 8010;
	private static ServerSocket socket;
	private static Socket client;

	public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws IOException {
		mcListener = new MCListener(mcAddress, mcPort);
		mdbListener = new MDBListener(mdbAddress, mdbPort);
		mdrListener = new MDRListener(mdrAddress, mdrPort);
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
	
	}

	public static void main(String args[]) throws InterruptedException, IOException, AlreadyBoundException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty ("java.rmi.server.hostname", "localhost");

		if (args.length != 9) {
			System.out.println("ERROR: Peer format : Peer <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT> <MC_IP> <MC_Port> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> ");
			return;
		}

		validateArgs(args);

		executor.execute(mcListener);
		executor.execute(mdbListener);
		executor.execute(mdrListener);
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

		TCPSocketPort += serverID;

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

	//protocols

	@Override
	public void backup(String filename, int repDegree) throws RemoteException, InterruptedException {
		File file = new File(filename);		
		
		String fileId = Utils.createFileId(file);
		
		if (memory.hasFileByID(fileId)){
				System.out.println("This file has already backed up!");
				return;
			}
			
		FileInfo fileInfo = new FileInfo(file,filename,repDegree);
		ArrayList<Chunk> chunks = fileInfo.getChunks();
		String chunkId;

		for (int i = 0; i < chunks.size(); i++) {
			
			byte[] header = Utils.getHeader("PUTCHUNK", protocolVersion, serverID, fileInfo.getFileId(), chunks.get(i).getChunkNo(), repDegree);
			String headerString = new String(header,0,header.length);
			
			System.out.println("\n SENT: " +headerString);

			chunkId = fileInfo.getFileId() + "-" + chunks.get(i).getChunkNo();

			if (!memory.hasFileByID(fileInfo.getFileId()))
				memory.files.add(fileInfo);

			if (!memory.savedOcurrences.containsKey(chunkId)) {
				memory.savedOcurrences.put(chunkId, 0);
			}

			byte[] body = chunks.get(i).getData();
			byte[] message = new byte[header.length + body.length];
			System.arraycopy(header, 0, message, 0, header.length);
			System.arraycopy(body, 0, message, header.length, body.length);
			System.out.println("message length: "+message.length);
			String channel = "mdb";
			Peer.executor.execute(new WorkerThread(message,channel));
			// The initiator-peer collects the confirmation			
			// messages during a time interval of one second
			Peer.executor.schedule(new BackupThread(chunkId, message, repDegree), 1, TimeUnit.SECONDS);
			
		}

	}

	@Override
	public void restore(String filename) throws RemoteException {
		File file = new File(filename);
		String name= file.getName();
		ArrayList<Chunk> chunks= new ArrayList<Chunk>();
		FileInfo fileInfo=null;
		String header = null;
		
		if (!memory.hasFileByName(name)) {
			System.out.println(filename + "has never backed up!");
			return;
		}else {
			String fileId = Utils.createFileId(file);
			System.out.println(fileId);
			for (int i =0;i <memory.files.size();i++) {
				if (memory.files.get(i).getFileId().equals(fileId)) {
					fileInfo= memory.files.get(i);
					chunks= memory.files.get(i).getChunks();
					break;
				}
			}
			for (int i = 0; i < chunks.size();i++) {
				header = "GETCHUNK "+ protocolVersion + " "+ serverID + " " +  fileInfo.getFileId()+ " "+ chunks.get(i).getChunkNo();
				header+= "\r\n\r\n";

				byte[] message = header.getBytes();

				System.out.println("\n SENT: "+header);
				String channel = "mc";
				Peer.executor.execute(new WorkerThread(message,channel));
			}
			Peer.executor.schedule(new RestoreFileThread(fileInfo.getFilename(),fileInfo.getFileId(),chunks.size()),10,TimeUnit.SECONDS);		
		}
	}
	

	@Override
	public void delete(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file,filename,0);

		if (!memory.hasFileByName(file.getName())) {
			System.out.println(filename + "has never backed up!");
			return;
		}

		String header = "DELETE " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + "\r\n\r\n";
		System.out.println("\n SENT: " + header);

		byte[] data;
		try {
			data = header.getBytes("US-ASCII");
			byte[] message = new byte[data.length];
			System.arraycopy(data, 0, message, 0, data.length);
			String channel = "mc";
			Peer.executor.execute(new WorkerThread(data,channel));
			// The initiator-peer collects the confirmation
			// messages during a time interval of one second
			Peer.executor.schedule(new DeleteThread(message), 1, TimeUnit.SECONDS);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
	}


	@Override
	public void reclaim(int space) throws RemoteException {
		int currentSpaceToFree = memory.getUsedMemory()-space; // space to free 
		
		if (currentSpaceToFree > 0) {
			
			List<String> sortedChunks=sortChunksToDelete();
			
			for (Iterator<String> iterator = sortedChunks.iterator(); iterator.hasNext();) {
				String[] splitString = iterator.next().trim().split(":");
				String key = splitString[0];
				if (currentSpaceToFree>0) {
					currentSpaceToFree-=memory.savedChunks.get(key).getChunkSize();
					String header = "REMOVED 1.0 "+serverID+" "+ memory.savedChunks.get(key).getFileId() + " "+memory.savedChunks.get(key).getChunkNo()+"\r\n\r\n";
					System.out.print(header);
					try {
						byte[] data = header.getBytes("US-ASCII");
						String channel = "mc";
						executor.execute(new WorkerThread(data,channel));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();	
					}
					String[] splitKey = key.trim().split("-");
					String filePath ="Peer"+Peer.getId()+"/"+"STORED"+"/"+ splitKey[0]+"/"+splitKey[1];
					System.out.println("filePath "+filePath);
					File fileToDelete = new File(filePath);
					boolean a=fileToDelete.delete();
					System.out.println("delete "+a);
					iterator.remove();	
					Peer.getMemory().savedOcurrences.put(key,Peer.getMemory().savedOcurrences.get(key)-1);
					Peer.getMemory().savedChunks.remove(key);
				}
					
					Peer.getMemory().capacity=space;
					Peer.getMemory().memoryUsed= Peer.getMemory().getUsedMemory();
					System.out.println("CAPACITY"+Peer.getMemory().capacity+" "+Peer.getMemory().memoryUsed);
			}
		}
		else {
			System.out.println("\nPeer "+ Peer.getId()+" doesn't have space to free!");
		}
		
	}
	


	@Override
	public void state() throws RemoteException {
		// TODO Auto-generated method stub
		int i;
		// Backup
		System.out.println("\nPEER "+Peer.getId()+" STATE");
		System.out.println("\nFor each file whose backup it has initiated:");
		for (i =0; i < memory.files.size();i++) {
			System.out.println("-File path: "+memory.files.get(i).getFilePath());
			System.out.println("-Backup service id of the file:"+memory.files.get(i).getFileId());
			System.out.println("-Replication degree:" + memory.files.get(i).getReplicationDegree());
			for (int j = 0; j< memory.files.get(i).getChunks().size();j++) {
				System.out.println("\n Backup chunks\n");
				System.out.println("--Chunk id: "+memory.files.get(i).getChunks().get(j).getChunkId());
				System.out.println("--Perceived replication degree: "+memory.savedOcurrences.get(memory.files.get(i).getChunks().get(j).getChunkId()));				
				
			}
		}
		//Stored chunks
		System.out.println("\n Stored chunks\n");
		for (String key: memory.savedChunks.keySet()) {
			System.out.println("--Chunk id: "+key);
			System.out.println("--Chunk size: "+memory.savedChunks.get(key).getChunkSize());
			System.out.println("--Perceived replication degree: "+memory.savedOcurrences.get(key));		
			
		}
		
		//Storage capacity
		System.out.println("\nThe maximum amount of disk space that can be used to store chunks: "+memory.capacity);
		System.out.println("The amount of storage used to backup the chunks: "+memory.memoryUsed);
		
	}


	//gets

	public static double getProtocolVersion(){return protocolVersion;}

	public static int getTCPPort(){return TCPSocketPort;}

	public static Memory getMemory() { return memory; }

	public static MDBListener getMDBListener() { return mdbListener; }

	public static ScheduledThreadPoolExecutor getExecutor() { return executor; }

	public static int getId() {return serverID; }

	public static MCListener getMCListener() { return mcListener; }

	public static MDRListener getMDRListener() { return mdrListener; }


	//utils
	public static void deleteLocalStorage() {
		String directory = "Peer"+Peer.getId();
		File file = new File(directory);
		if (!file.exists()) {
			System.out.println(directory +" does not exist");
		}
		else System.out.println("exist");
	}

	public static List<String> sortChunksToDelete() {
		ArrayList<String> chunksToSort = new ArrayList<String>();
		for (String key : memory.savedChunks.keySet()){
			int diff = memory.savedOcurrences.get(key)- memory.savedChunks.get(key).getReplicationDegree(); 
			String chunk = key +":"+diff;
			chunksToSort.add(chunk);
		}
		chunksToSort.sort((o1, o2) -> {
			int chunk1 = Integer.valueOf(o1.split(":")[1]);
			int chunk2 = Integer.valueOf(o2.split(":")[1]);
			return Integer.compare(chunk1, chunk2);
		});

		List<String> returnList =chunksToSort;
		Collections.reverse(returnList);
		System.out.println(returnList);
		return returnList;
	}
}