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
            Socket socket = serverSocket.accept();
            while (true) {
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                System.out.println("connect");
                int i = dataInputStream.available();
                StringBuilder sb = new StringBuilder();
                char c;
                while ((c = dataInputStream.readChar())!= '\n') {
                    //System.out.println("\n"+i+"\n");
                    sb.append(c);
                }
              String message = sb.toString();
              analizeMessage(message);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void analizeMessage(String message){

        String[] splitMessage = message.trim().split("\\s+");
        switch (splitMessage[0]){
            case "BACKUP":
            System.out.println("backup");
            break;
            default:

        }
    }
}