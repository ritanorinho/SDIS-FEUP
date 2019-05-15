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
              if (analizeMessage(message)){
                String[] splitMessage = message.trim().split("\\s+");
                String peerID = splitMessage[0];
                Server.getMemory().addConnection(peerID, socket);
                System.out.println("List of Peers Connected: ");
                System.out.println(Collections.singletonList(Server.getMemory().conections));
              }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static boolean analizeMessage(String message){

        String[] splitMessage = message.trim().split("\\s+");

        String substr = splitMessage[0].substring(0,4);
        if (substr.equals("Peer")){
            System.out.println("PEER");
            return true;
        }
        else{
            switch (splitMessage[0]){
                case "BACKUP":
                System.out.println("backup");
                break;
                default:
    
            }
        }
        return false;
    }
}