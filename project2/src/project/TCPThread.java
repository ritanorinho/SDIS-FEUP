package project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPThread extends Thread {

    TCPThread() {}

    @Override
    public void run() 
    {
        ServerSocket serverSocket = Server.getServerSocket();

        while(true)
        {
            try 
            {
                Socket socket = serverSocket.accept();

                //Receive peer id and add to concurrent hash map
                
            } catch (IOException e) {}
        }
    }
}