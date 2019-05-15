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
import java.util.Collections;

public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    private String message;
    private int i = 0;

    TCPThread(String message) {
        this.message = message;
        
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

    public static boolean analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
       
        String substr = splitMessage[0].substring(0, 4);
        if (substr.equals("Peer")) {
            System.out.println("PEER");
            return true;
        } else {
            switch (splitMessage[0]) {
            case "BACKUP":
                System.out.println("backup");
                break;
            default:

            }
        }
        return false;
    }
}