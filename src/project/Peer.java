package project;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
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

public class Peer implements RMIInterface {

	private static double protocolVersion;
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

		{
			if (args.length != 9) {
				System.out.println(
						"ERROR: Peer format : Peer <MC_IP> <MC_Port> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT>");
				return;
			} else
				validateArgs(args);

			executor.execute(mcListener);
			executor.execute(mdbListener);
			executor.execute(mdrListener);


		}
		
	}

	private static void validateArgs(String[] args)
			throws RemoteException, InterruptedException, IOException, AlreadyBoundException {

		InetAddress MCAddress = InetAddress.getByName(args[0]);
		Integer MCPort = Integer.parseInt(args[1]);
		InetAddress MDBAddress = InetAddress.getByName(args[2]);
		Integer MDBPort = Integer.parseInt(args[3]);
		InetAddress MDRAddress = InetAddress.getByName(args[4]);
		Integer MDRPort = Integer.parseInt(args[5]);
		protocolVersion = Double.parseDouble(args[6]);
		serverID = Integer.parseInt(args[7]);
		accessPoint = args[8];

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

	public static Memory getMemory() {
		return memory;
	}

	public static MDBListener getMDBListener() {
		return mdbListener;
	}

	public static ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	@Override
	public void backup(String filename, int repDegree) throws RemoteException, InterruptedException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file,filename,repDegree);
		System.out.println("filename:" +fileInfo.getFilename());
		memory.files.add(fileInfo);
		memory.filenameId.put(fileInfo.getFileId(), fileInfo.getFilename());
		
		ArrayList<Chunk> chunks = fileInfo.getChunks();
		String name;

		for (int i = 0; i < chunks.size(); i++) {
			String header = "PUTCHUNK " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + " "
					+ chunks.get(i).getChunkNo() + " " + repDegree + "\n\r\n\r";
			
			
			System.out.println("\n SENT: " + header);

			name = fileInfo.getFileId() + "-" + chunks.get(i).getChunkNo();

			if (!memory.files.contains(fileInfo))
				memory.files.add(fileInfo);

			if (!memory.savedOcurrences.containsKey(name)) {
				memory.savedOcurrences.put(name, 0);
			}

			byte[] data;
			try {
				data = header.getBytes("US-ASCII");
				
				byte[] body = chunks.get(i).getData();
				byte[] message = new byte[data.length + body.length];
				System.arraycopy(data, 0, message, 0, data.length);
				System.arraycopy(body, 0, message, data.length, body.length);
				System.out.println("message length: "+message.length);
				String channel = "mdb";
				Peer.executor.execute(new WorkerThread(message,channel));
				// The initiator-peer collects the confirmation			
				// messages during a time interval of one second
				Peer.executor.schedule(new BackupThread(name, message, repDegree), 1, TimeUnit.SECONDS);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		}

	}

	@Override
	public void restore(String filename) throws RemoteException {
		File file = new File(filename);
		String name= file.getName();
		ArrayList<Chunk> chunks= new ArrayList<Chunk>();
		FileInfo fileInfo=null;
		if (!memory.filenameId.containsValue(name)) {
			System.out.println(filename + "has never backed up!");
		}else {
			for (int i =0;i <memory.files.size();i++) {
				if (memory.files.get(i).getFilename().equals(name)) {
					fileInfo= memory.files.get(i);
				
					chunks= memory.files.get(i).getChunks();
					break;
				}
			}
		for (int i = 0; i < chunks.size();i++) {
			String header = "GETCHUNK "+ protocolVersion + " "+ serverID + " " +  fileInfo.getFileId()+ " "+ chunks.get(i).getChunkNo() + "\n\r\n\r";
			System.out.println("\n SENT: "+header);
				String channel = "mc";
			try {
				Peer.executor.execute(new WorkerThread(header.getBytes("US-ASCII"),channel));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		Peer.executor.schedule(new RestoreFileThread(fileInfo.getFilename(),fileInfo.getFileId()),10,TimeUnit.SECONDS);
		}
	}

	@Override
	public void delete(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file,filename,0);

		if (!memory.hasFile(fileInfo.getFileId())) {
			System.out.println("File is not on the system");
			return;
		}

		String header = "DELETE " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + "\n\r\n\r";
		System.out.println("\n SENT: " + header);

		byte[] data;
		try {
			data = header.getBytes("US-ASCII");
			byte[] message = new byte[data.length];
			System.arraycopy(data, 0, message, 0, data.length);
			String channel = "mc";
			Peer.executor.execute(new WorkerThread(data,channel));
			mcListener.message(message);
			// The initiator-peer collects the confirmation
			// messages during a time interval of one second
			Peer.executor.schedule(new DeleteThread(message), 1, TimeUnit.SECONDS);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}


	@Override
	public void reclaim(int space) throws RemoteException {
		int currentSpaceToFree = memory.getUsedMemory()-space; // space to free 
		
		if (currentSpaceToFree > 0) {
			ArrayList<String> sortedChunks=sortChunksToDelete();
			for (Iterator<String> iterator = sortedChunks.iterator(); iterator.hasNext();) {
				String[] splitString = iterator.next().trim().split("-");
				String key = splitString[0]+"-"+splitString[1];
				if (currentSpaceToFree>0) {
					currentSpaceToFree-=memory.savedChunks.get(key).getChunkSize();
					String header = "REMOVED 1.0 "+serverID+" "+ memory.savedChunks.get(key).getFileId() + " "+memory.savedChunks.get(key).getChunkNo()+"\n\r\n\r";
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
					System.out.println("-"+Peer.getMemory().savedOcurrences.get(key));
					Peer.getMemory().savedOcurrences.put(key,Peer.getMemory().savedOcurrences.get(key)-1);
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

	public static int getId() {
		// TODO Auto-generated method stub
		return serverID;
	}

	public static MCListener getMCListener() {
		// TODO Auto-generated method stub
		return mcListener;
	}

	public static MDRListener getMDRListener() {
		// TODO Auto-generated method stub
		return mdrListener;
	}

public static void deleteLocalStorage() {
	String directory = "Peer"+Peer.getId();
	File file = new File(directory);
	if (!file.exists()) {
		System.out.println(directory +" does not exist");
	}
	else System.out.println("exist");
}


public static ArrayList<String> sortChunksToDelete() {
	ArrayList<String> chunksToSort = new ArrayList<String>();
	for (String key : memory.savedChunks.keySet()){
		String chunk = key +"-"+memory.savedChunks.get(key).getReplicationDegree();
		chunksToSort.add(chunk);
	}
	chunksToSort.sort((o1, o2) -> {
        int chunk1 = Integer.valueOf(o1.split("-")[2]);
        int chunk2 = Integer.valueOf(o2.split("-")[2]);
        return Integer.compare(chunk1, chunk2);
    });
	System.out.println(chunksToSort);
	return chunksToSort;
}
}