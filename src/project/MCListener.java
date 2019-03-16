package project;

import java.net.InetAddress;
import java.util.Scanner;

public class MCListener extends Listener {

	 public MCListener(InetAddress mcAddress, Integer mcPort) {
			super(mcAddress,mcPort);
		
	}
	 public MCListener() {
		// TODO Auto-generated constructor stub
	}

	    // Sleeps for some time and waits for a key press. After key 
	    // is pressed, it notifies produce(). 
	    public void consume()throws InterruptedException 
	    { 
	        // this makes the produce thread to run first. 
	        Thread.sleep(1000); 
	        Scanner s = new Scanner(System.in); 

	        // synchronized block ensures only one thread 
	        // running at a time. 
	        synchronized(this) 
	        { 
	            System.out.println("Waiting for return key."); 
	            s.nextLine(); 
	            System.out.println("Return key pressed"); 

	            // notifies the produce thread that it 
	            // can wake up. 
	            notify(); 

	            // Sleep 
	            Thread.sleep(2000); 
	        } 
	    }

	
}
