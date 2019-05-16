package project;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;

import utils.Memory;

public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    private ScheduledThreadPoolExecutor executor;
    private Memory memory;

    public TCPThread(SSLServerSocket serverSocket, ScheduledThreadPoolExecutor executor, Memory memory) {
        this.executor = executor;
        this.memory = memory;
        this.serverSocket = serverSocket;
        
    }

	@Override
    public void run() {
        try {
            while(true){
                
                Socket socket = this.serverSocket.accept();
                executor.execute(new AcceptConnectionsThread(socket,executor, memory));
            
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}