package project;

import java.util.Scanner;

public class MCListener extends Listener implements Runnable{

	 public void produce()throws InterruptedException 
	    { 
	        // synchronized block ensures only one thread 
	        // running at a time. 
	        synchronized(this) 
	        { 
	            System.out.println("producer thread running"); 

	            // releases the lock on shared resource 
	            wait(); 

	            // and waits till some other method invokes notify(). 
	            System.out.println("Resumed"); 
	        } 
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

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
}
