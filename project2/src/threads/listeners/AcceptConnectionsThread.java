package threads.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import app.Server;

public class AcceptConnectionsThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;

    public AcceptConnectionsThread(Socket socket, ScheduledThreadPoolExecutor executor) {
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

                if(!analize.equals("")){
                pwrite.println(analize);
                pwrite.flush();
                }
                else System.out.println("Null message");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
        String substr = splitMessage[0].trim().substring(0, 4);
        String peer;

        if (substr.equals("Peer"))
            return "connected";
        
        switch(splitMessage[0].trim()) 
        {
            case "BACKUP":
                peer = splitMessage[2];
                int replicationDegree = Integer.parseInt(splitMessage[3]);
                return getOtherPeers(peer,replicationDegree);

            case "STORED":

                String peerID = splitMessage[1];
                String chunkID = splitMessage[2];
                String key = chunkID +"-"+peerID;   
                System.out.println("Peer id "+peerID);
                Server.getMemory().serverSavedChunks.add(key);
                break;

            case "DELETE":
                peer = splitMessage[2];
                String file = splitMessage[1];
                return getPeersWithFile(file);

            default:
                System.out.println("Unknown message: " + splitMessage[0].trim());
        }   
        return "";
    }

    private String getPeersWithFile(String file) {
        StringBuilder sb = new StringBuilder();
        String conectionPorts = "";

        System.out.println(file);
        for (int i = 0; i < Server.getMemory().serverSavedChunks.size();i++){
            String[] split = Server.getMemory().serverSavedChunks.get(i).split("-");
            String fileId = split[0].trim();
            System.out.println(Server.getMemory().serverSavedChunks.get(i));
            if (fileId.equals(file)){
                String peer = split[2].trim();
                sb.append(Server.getMemory().conectionsPorts.get(peer));
                sb.append(" ");
            }
        }
        conectionPorts = sb.toString();

        return conectionPorts;
    }

    public static String getOtherPeers(String peer, int replicationDegree) {
        StringBuilder sb = new StringBuilder();
        String conectionPorts = "";
        
        for (String key : Server.getMemory().conectionsPorts.keySet()) {
            if (replicationDegree > 0) {
                if (!key.equals(peer)) { //TODO Check if other peer doesn't have chunk already
                    sb.append(Server.getMemory().conectionsPorts.get(key));
                    sb.append(" ");
                    replicationDegree--;
                }
            } else
                break;

        }

        conectionPorts = sb.toString();
        
        return conectionPorts;
    }
}