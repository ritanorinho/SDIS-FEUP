package project;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;


public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    private ScheduledThreadPoolExecutor executor;

    public TCPThread(SSLServerSocket serverSocket, ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
        this.serverSocket = serverSocket;
        
    }

	@Override
    public void run() {
        try {
            while(true){
                
                Socket socket = this.serverSocket.accept();
                executor.execute(new AcceptConnectionsThread(socket,executor));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}