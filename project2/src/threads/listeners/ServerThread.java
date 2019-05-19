package threads.listeners;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLServerSocket;


public class ServerThread extends Thread {
    private SSLServerSocket serverSocket;
    private ScheduledThreadPoolExecutor executor;

    public ServerThread(SSLServerSocket serverSocket, ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
        this.serverSocket = serverSocket;
    }

	@Override
    public void run() {
        try {
            while(true){
                
                Socket socket = this.serverSocket.accept();
                executor.execute(new ServerListenerThread(socket,executor));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}