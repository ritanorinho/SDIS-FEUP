package project;
import java.util.Scanner;  
public class Peer {

	private int protocolVersion;
	private int serverID;
	private String accessPoint;

	

	 public static void main(String args[])throws InterruptedException  {
                       
		     { 
		    	 
		    	 Listener listener = new Listener();
		   
		         // Create a thread object that calls pc.produce() 
		         Thread t1 = new Thread(new Runnable() 
		         { 
		             @Override
		             public void run() 
		             { 
		                 try
		                 { 
		                	 if (args[0]=="1") {
		                		 //listener = new MCListener();
		                		 listener.produce();
		                	 }
		                     
		                listener.consume();
		                 } 
		                 catch(InterruptedException e) 
		                 { 
		                     e.printStackTrace(); 
		                 } 
		             } 
		         }); 
		  
		         // Start both threads 
		         t1.start(); 
		         t1.join(); 
		     } 
	 }
}