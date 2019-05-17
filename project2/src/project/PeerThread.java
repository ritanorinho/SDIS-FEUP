package project;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;


public class PeerThread extends Thread {
    private SSLServerSocket peerSocket;
    private ScheduledThreadPoolExecutor executor;

    public PeerThread(SSLServerSocket peerSocket,ScheduledThreadPoolExecutor executor) {
       
        this.peerSocket = peerSocket;
        this.executor = executor;
	}

	@Override
    public void run() {
        try {
           
            while(true){
                Socket socket = peerSocket.accept();
                executor.execute(new PeersCommunicationThread(socket,executor));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}