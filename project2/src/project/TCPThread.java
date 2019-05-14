package project;

import java.io.BufferedReader;
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
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream;
                DataInputStream dataInputStream;
                inputStream = socket.getInputStream();
                dataInputStream = new DataInputStream(inputStream);
                System.out.println("connect");
                while (true) {
                    int length = dataInputStream.readInt();
                    byte[] data = new byte[length];
                    dataInputStream.read(data, 0, length);
                    System.out.println(" " + length);

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}