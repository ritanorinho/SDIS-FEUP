package project;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;

import utils.Memory;

public class PeerThread extends Thread {
    private SSLServerSocket peerSocket;
    private ScheduledThreadPoolExecutor executor;
    private Memory memory;

    public PeerThread(SSLServerSocket peerSocket) {
       
        this.peerSocket = peerSocket;
	}

	@Override
    public void run() {
        try {
            while(true){
                Socket socket = peerSocket.accept();
                System.out.println("created peer socket");
                Server.executor.execute(new AcceptConnectionsThread(socket,executor, memory));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}