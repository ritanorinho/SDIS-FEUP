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
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AcceptConnectionsThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;
    private int i = 0;

    AcceptConnectionsThread(Socket socket,ScheduledThreadPoolExecutor executor){
       this.socket = socket;
       this.executor = executor;
    }

    @Override
    public void run() {
        try {
            
            while (true) {
                System.out.println("inside run");
                
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                System.out.println("connect");
                StringBuilder sb = new StringBuilder();
                char c;
                while ((c = dataInputStream.readChar()) != '\n') {
                    sb.append(c);
                }
                String message = sb.toString();
                System.out.println("message " + message);
                if (analizeMessage(message)) {
                    String[] splitMessage = message.trim().split("\\s+");
                    String peerID = splitMessage[0];
                    Integer port = Integer.parseInt(splitMessage[1]);
                    String address = splitMessage[2];
                    Server.getMemory().addConnection(peerID, socket,port,address);
                    System.out.println("List of Peers Connected: ");
                    System.out.println(Collections.singletonList(Server.getMemory().conections));
                }
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