package project;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;

public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    TCPThread() {
        serverSocket = Server.getServerSocket();
    }

    @Override
    public void run() {
        try {
            while(true){
                
                Socket socket = serverSocket.accept();
                Server.executor.execute(new AcceptConnectionsThread(socket,Server.executor));
            
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}