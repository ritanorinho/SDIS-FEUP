package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AcceptConnectionsThread extends Thread {
    private Socket socket;
    private  ScheduledThreadPoolExecutor executor;

    public  AcceptConnectionsThread(Socket socket, ScheduledThreadPoolExecutor executor) {
        this.socket = socket;
        this.executor = executor;
    }

    @Override
    public void run() {
        try {

            String message;
            String analize = null;
            while (true) {
    
                OutputStream ostream = socket.getOutputStream();
                PrintWriter pwrite = new PrintWriter(ostream, true);
               
                InputStream istream = socket.getInputStream();
                BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

                if ((message = receiveRead.readLine()) != null) {
                    System.out.println("\nReceived message " + message);
                    analize = analizeMessage(message);
                }

                if (analize.equals("connected")) {
                    String[] splitMessage = message.trim().split("\\s+");
                    String peerID = splitMessage[0];
                    Integer port = Integer.parseInt(splitMessage[1]);
                    String address = splitMessage[2];
                    Server.getMemory().addConnection(peerID, socket, port, address);
                    System.out.println("List of Peers Connected: ");
                    System.out.println(Collections.singletonList(Server.getMemory().conections));
                }
                pwrite.println(analize);
                pwrite.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
        String substr = splitMessage[0].trim().substring(0, 4);

        if (substr.equals("Peer")) {
            return "connected";
        } else {
            switch (splitMessage[0].trim()) {
            case "BACKUP":
                String peer = splitMessage[2];
                int replicationDegree = Integer.parseInt(splitMessage[3]);
                String otherPeer;
               return getOtherPeers(peer,replicationDegree);
            case "STORED":
               String peerID = splitMessage[1];
               String chunkID = splitMessage[2];   
               Server.getMemory().serverSavedChunks.put(chunkID, peerID);
                break;
            case "DELETE":
            
            break;
            default:

            }
        }
        return null;
    }

    public static String getOtherPeers(String peer, int replicationDegree) {
        StringBuilder sb = new StringBuilder();
        String conectionPorts = "";
        for (String key : Server.getMemory().conectionsPorts.keySet()) {
            if(replicationDegree > 0){
            if (!key.equals(peer)) {
                sb.append(Server.getMemory().conectionsPorts.get(key));
                sb.append(" ");
                replicationDegree--;
            }
        } else break;


        }
        conectionPorts = sb.toString();
        return conectionPorts;
    }
}