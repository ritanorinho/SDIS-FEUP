package project;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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
	private static MulticastSocket socket;
	private static ScheduledThreadPoolExecutor executor;
	private static Memory memory = new Memory();

	public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws IOException {
		mcListener = new MCListener(mcAddress, mcPort);
		mdbListener = new MDBListener(mdbAddress, mdbPort);
		mdrListener = new MDRListener(mdrAddress, mdrPort);
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
		executor.execute(mcListener);
		executor.execute(mdbListener);
		executor.execute(mdrListener);

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

			socket = new MulticastSocket();

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
		FileInfo fileInfo = new FileInfo(file);
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

			if (!memory.backupChunks.containsKey(name)) {
				memory.backupChunks.put(name, 0);
			}

			try {
				byte[] data = header.getBytes();
				byte[] body = chunks.get(i).getData();
				byte[] message = new byte[data.length + body.length];
				System.arraycopy(data, 0, message, 0, data.length);
				System.arraycopy(body, 0, message, data.length, body.length);
				String channel = "mdb";
				String worker = message + "-" + channel;
				Peer.executor.execute(new WorkerThread(worker));
				mdbListener.message(message);
				Thread.sleep(500);
				// The initiator-peer collects the confirmation
				// messages during a time interval of one second
				Peer.executor.schedule(new BackupThread(name, message, repDegree), 1, TimeUnit.SECONDS);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void restore(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file);
		System.out.println("filename: " +fileInfo.getFilename());
		ArrayList<Chunk> chunks= fileInfo.getChunks();
		String name;
		System.out.println(memory.filenameId.size());
		if (!memory.filenameId.containsValue(fileInfo.getFilename())) {
			System.out.println(filename + "has never backed up!");
		}else {
		for (int i = 0; i < chunks.size();i++) {
			String header = "GETCHUNK "+ protocolVersion + " "+ serverID + " " +  fileInfo.getFileId()+ " "+ chunks.get(i).getChunkNo() + "\n\r\n\r";
			System.out.println("\n SENT: "+header);
			
			name= fileInfo.getFileId()+"-"+chunks.get(i).getChunkNo();

			if (!memory.backupChunks.containsKey(name)) {
				Peer.memory.backupChunks.put(name,0);
			}
			
			String channel = "mc";
			String worker = header + "-"+channel;
			Peer.executor.execute(new WorkerThread(worker));
			
		}
		}
		
	}

	@Override
	public void delete(String filename) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file);

		if (!memory.hasFile(fileInfo.getFileId())) {
			System.out.println("File is not on the system");
			return;
		}

		String header = "DELETE " + protocolVersion + " " + serverID + " " + fileInfo.getFileId() + "\n\r\n\r";
		System.out.println("\n SENT: " + header);

		byte[] data = header.getBytes();
		byte[] message = new byte[data.length];
		System.arraycopy(data, 0, message, 0, data.length);
		String channel = "mc";
		String worker = message + "-" + channel;

		try {
			Peer.executor.execute(new WorkerThread(worker));
			mcListener.message(message);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// The initiator-peer collects the confirmation
		// messages during a time interval of one second
		Peer.executor.schedule(new DeleteThread(message), 1, TimeUnit.SECONDS);
	}


	@Override
	public void reclaim(int space) throws RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void state() throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("ABC");
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

	}