package project;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ScheduledThreadPoolExecutor;  
public class Peer implements RMIInterface {

	private static double protocolVersion;
	private static int serverID;
	private static String accessPoint;
	private static volatile MCListener mcListener;
	private static volatile MDBListener mdbListener;
	private static volatile MDRListener mdrListener;
	private static MulticastSocket socket;
	private static ScheduledThreadPoolExecutor executor;
	private static Memory memory;
	

	

	 public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws IOException {
		 mcListener= new MCListener(mcAddress,mcPort);
		 mdbListener = new MDBListener(mdbAddress,mdbPort);
		 mdrListener = new MDRListener(mdrAddress, mdrPort);
		 MulticastSocket mcSocket = new MulticastSocket();
		 mcSocket.setTimeToLive(1);
		 DatagramPacket packet;

		 String test;
		 test = "mc test";
		 packet = new DatagramPacket(test.getBytes(), test.getBytes().length,
					mcAddress, mcPort);
			mcSocket.send(packet);
			mcSocket.close();
		 mcSocket = new MulticastSocket();
		 mcSocket.setTimeToLive(1);
		
		 mcSocket = new MulticastSocket();
		 mcSocket.setTimeToLive(1);
		 test = "mdr test";
		 packet = new DatagramPacket(test.getBytes(), test.getBytes().length,
					mdrAddress, mdrPort);
		
			mcSocket.send(packet);
		mcSocket.close();
		 
		 new Thread(mcListener).start();
		 new Thread(mdbListener).start();
		 new Thread(mdrListener).start();
		 
		
		}
		 
		   
	public static void main(String args[])throws InterruptedException, IOException, AlreadyBoundException  {
                       
		 {
			 if (args.length != 9) {
				 System.out.println("ERROR: Peer format : Peer <MC_IP> <MC_Port> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT>");
				 return;
			 }
			 else 
				 validateArgs(args);
			 		
		   socket = new MulticastSocket();
		   

		 }
		 }

	private static void validateArgs(String[] args) throws InterruptedException, IOException, AlreadyBoundException {
		InetAddress MCAddress= InetAddress.getByName(args[0]);
		Integer MCPort = Integer.parseInt(args[1]);
		InetAddress MDBAddress= InetAddress.getByName(args[2]);
		Integer MDBPort = Integer.parseInt(args[3]);
		InetAddress MDRAddress= InetAddress.getByName(args[4]);
		Integer MDRPort = Integer.parseInt(args[5]);
		protocolVersion= Double.parseDouble(args[6]);
		serverID= Integer.parseInt(args[7]);
		accessPoint=args[8];
		
		Peer peer = new Peer(MCAddress,MCPort,MDBAddress,MDBPort,MDRAddress,MDRPort);
		RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(peer, 0);
		Registry registry = LocateRegistry.createRegistry(1099);
        registry.bind(accessPoint, stub);
		
	}


	@Override
	public void backup(String filename, int repDegree) throws RemoteException {
		File file = new File(filename);
		FileInfo fileInfo = new FileInfo(file);
		ArrayList<Chunk> chunks= fileInfo.getChunks();
		System.out.println(chunks.size());
				
		for (int i = 0; i < chunks.size();i++) {
			String header = "PUTCHUNK "+ protocolVersion + " "+ serverID + " " +  fileInfo.getFileId()+ " "+ chunks.get(i).getChunkNo() + " "+repDegree + "\n\r\n\r";
			System.out.println("SENT "+header);
			
			String name= fileInfo.getFileId()+"-"+chunks.get(i).getChunkNo();
			if (!memory.storedChunks.containsKey(name)) {
				Peer.memory.storedChunks.put(name,0);
			}
			
			try {
				byte[] data = chunks.get(i).getData();
				mdbListener.message(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			
		}
		
		
		
	}


	@Override
	public void restore(String file) throws RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void delete(String file) throws RemoteException {
		// TODO Auto-generated method stub
		
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
	
	}