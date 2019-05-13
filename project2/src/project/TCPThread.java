package project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class TCPThread extends Thread {
    ServerSocket serverSocket;
    private String message;
    TCPThread(String message) {
        this.message = message;
        System.out.println("tcp thread");
        serverSocket = Server.getServerSocket();
    }

    @Override
    public void run() {
        

        
            try {
                Socket socket = serverSocket.accept();
                System.out.println("connect");
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String number = br.readLine();
                System.out.println("Message received from client is "+number);
                socket.close();


            } catch (IOException e) {
            }
    }
   
}