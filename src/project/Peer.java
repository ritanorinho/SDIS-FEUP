package project;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ScheduledThreadPoolExecutor;  
public class Peer {

	private static double protocolVersion;
	private static int serverID;
	private static String accessPoint;
	private static volatile MCListener mcListener;
	private static volatile MDBListener mdbListener;
	private static volatile MDRListener mdrListener;
	private static MulticastSocket socket;
	private static ScheduledThreadPoolExecutor executor;

	

	 public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws InterruptedException, IOException{
		 mcListener= new MCListener(mcAddress,mcPort);
		 mdbListener = new MDBListener(mdbAddress,mdbPort);
		 mdrListener = new MDRListener(mdrAddress, mdrPort);
		 
		 new Thread(mcListener).start();
		 new Thread(mdbListener).start();
		 new Thread(mdrListener).start();
		 
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
		 test = "mdb test";
		 packet = new DatagramPacket(test.getBytes(), test.getBytes().length,
					mdbAddress, mdbPort);
			mcSocket.send(packet);
			mcSocket.close();
		 mcSocket = new MulticastSocket();
		 mcSocket.setTimeToLive(1);
		 test = "mdr test";
		 packet = new DatagramPacket(test.getBytes(), test.getBytes().length,
					mdrAddress, mdrPort);
		
			mcSocket.send(packet);
		mcSocket.close();
		}
		 
		   
	public static void main(String args[])throws InterruptedException, IOException  {
                       
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

	private static void validateArgs(String[] args) throws InterruptedException, IOException {
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
		
	}
	
	}