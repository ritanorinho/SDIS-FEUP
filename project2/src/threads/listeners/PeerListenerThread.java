package threads.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import app.Server;

public class PeerListenerThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;
    private boolean connected = false;

    public PeerListenerThread(Socket socket, ScheduledThreadPoolExecutor executor) {
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

                if (!analize.equals("")) {
                    pwrite.println(analize);
                    pwrite.flush();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
        String peer, peerID, file;

        if (!splitMessage[0].equals("Peer") && !connected)
            return "";

        switch (splitMessage[0].trim()) {
        case "Peer":
            int port = Integer.parseInt(splitMessage[2]);

            peerID = splitMessage[1];

            Server.getMemory().addConnection(peerID, socket, port);
            connected = true;

            System.out.println("New peer connected: Peer" + peerID + "@" + socket.getInetAddress() + ":" + port);

            return "connected";

        case "BACKUP":
            peer = splitMessage[2];
            return getAvailablePeers(peer, Integer.parseInt(splitMessage[3]), splitMessage[1]);

        case "STORED":
            peer = splitMessage[1];
            String chunkID = splitMessage[2];
            String key = chunkID + "-" + peer;
            System.out.println("Peer id " + peer);
            Server.getMemory().serverSavedChunks.add(key);
            break;

        case "DELETE":
            peer = splitMessage[2];
            file = splitMessage[1];
            return getPeersWithFile(file);
        case "DELETED":
            peer = splitMessage[2];
            file = splitMessage[1];
            deleteChunks(peer, file);
            break;

        default:
            System.out.println("Unknown message: " + splitMessage[0].trim());
        }

        return "";
    }

    private void deleteChunks(String peer, String file) {
        List<String> serverSavedChunks = Server.getMemory().serverSavedChunks;
        for (Iterator<String> iterator = serverSavedChunks.iterator(); iterator.hasNext();) {
            String[] key = iterator.next().trim().split("-");
            String file1 = key[0];
            String peer1 = key[2];
            if (file.equals(file1) && peer.equals(peer1))
                iterator.remove();

        }
    }

    private String getPeersWithFile(String file) {
        StringBuilder sb = new StringBuilder();
        String conectionPorts = "";
        HashMap<String, String> peerPorts = new HashMap<String, String>();

        System.out.println(file);
        for (int i = 0; i < Server.getMemory().serverSavedChunks.size(); i++) {
            String[] split = Server.getMemory().serverSavedChunks.get(i).split("-");
            String fileId = split[0].trim();
            System.out.println(Server.getMemory().serverSavedChunks.get(i));
            if (fileId.equals(file)) {
                String peer = split[2].trim();
                if (!peerPorts.containsKey(peer)) {
                    sb.append(Server.getMemory().getPeerPort(peer));
                    sb.append(" ");
                    peerPorts.put(peer, Server.getMemory().getPeerPort(peer));
                }
            }
        }
        conectionPorts = sb.toString();
        System.out.println("conection ports " + conectionPorts);

        return conectionPorts;
    }

    public static String getAvailablePeers(String peer, int replicationDegree, String chunckId) {
        String sb = "";

        for (String key : Server.getMemory().conections.keySet()) {
            if (replicationDegree > 0) {
                if (!key.equals(peer)) { // TODO Check if other peer doesn't have chunk already => Better algorythm
                                         // maybe?
                    sb += Server.getMemory().conections.get(key).getKey().getInetAddress().getHostAddress() + "-"
                            + Server.getMemory().conections.get(key).getValue() + " ";
                    replicationDegree--;
                }
            } else
                break;

        }

        return sb;
    }
}