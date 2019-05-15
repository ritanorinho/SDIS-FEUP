package project;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.PrintWriter;
import javax.net.ssl.SSLServerSocket;

public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    private String message;

    TCPThread(String message) {
        this.message = message;
        System.out.println("tcp thread");
        serverSocket = Server.getServerSocket();
    }

    @Override
    public void run() {
        try {
            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            int i = 0;
            while (true) {
                Socket socket = serverSocket.accept();
               
               DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                System.out.println("connect");
                byte[] data = new byte[65000];
                int length = -1;
                socket.setReceiveBufferSize(65000);
                while ((length = dataInputStream.read(data)) > 0) {
                    
                    System.out.println("---"+socket.getReceiveBufferSize());
                    System.out.println(length);
                    //int length = dataInputStream.read(data);
                    bufferStream.write(data, 0, length);
                    String dataString = new String(data);

                    //System.out.println(i+"\n " + dataString+"\n\n");
                }
                //System.out.println(data.toString());
                System.out.println("abc");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}