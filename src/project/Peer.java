package project;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;  
public class Peer {

	private static double protocolVersion;
	private static int serverID;
	private static String accessPoint;
	private static volatile MCListener mcListener;
	private static volatile MDBListener mdbListener;
	private static volatile MDRListener mdrListener;

	

	 public Peer(InetAddress mcAddress, Integer mcPort, InetAddress mdbAddress, Integer mdbPort, InetAddress mdrAddress,
			Integer mdrPort) throws InterruptedException {
		 mcListener= new MCListener(mcAddress,mcPort);
		 mdbListener = new MDBListener(mdbAddress,mdbPort);
		 mdrListener = new MDRListener(mdrAddress, mdrPort);
		 
		   
		 Thread t1 = new Thread(new Runnable() 
	        { 
	            @Override
	            public void run() 
	            { 
	                try
	                { 
	                    mcListener.consume(); 
	                } 
	                catch(InterruptedException e) 
	                { 
	                    e.printStackTrace(); 
	                } 
	            } 
	        }); 
	  
	      	        t1.start(); 

	  
	        // t1 finishes before t2 
	        t1.join(); 
	    } 

	public static void main(String args[])throws InterruptedException, UnknownHostException  {
                       
		 {
			 if (args.length != 9) {
				 System.out.println("ERROR: Peer format : Peer <MC_IP> <MC_Port> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> <PROTOCOL_VERSION> <SERVER_ID> <SERVICE_ACCESS_POINT>");
				 
			 }
			 else 
				 validateArgs(args);
		    	 
		   	 }
}

	private static void validateArgs(String[] args) throws InterruptedException, UnknownHostException {
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