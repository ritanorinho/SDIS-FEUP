package project;

import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import javax.net.ssl.SSLServerSocket;

public class TCPThread extends Thread 
{
    private SSLServerSocket serverSocket;
    private String message;

    TCPThread(String message) 
    {
        this.message = message;
        System.out.println("tcp thread");
        serverSocket = Server.getServerSocket();
    }

    @Override
    public void run() 
    {
        try 
        {
            while(true)
            {
                Socket socket = serverSocket.accept();
                System.out.println("connect");
    
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("smt");
            }
            
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
   
}