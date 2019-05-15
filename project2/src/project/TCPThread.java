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

    public TCPThread(ScheduledThreadPoolExecutor executor, Memory memory) {
        this.executor = executor;
        this.memory = memory;
        serverSocket = Server.getServerSocket();
	}

	@Override
    public void run() {
        try {
            while(true){
                
                Socket socket = serverSocket.accept();
                Server.executor.execute(new AcceptConnectionsThread(socket,executor, memory));
            
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}