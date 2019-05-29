package threads.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import app.Server;

public class ServerListenerThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;
    private boolean connected = false;
    private String peerId;

    public ServerListenerThread(Socket socket, ScheduledThreadPoolExecutor executor) {
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

                if (analize.equals("end"))
                    continue;

                if (!analize.equals("")) {
                    pwrite.println(analize);
                    pwrite.flush();
                }
               
            }

        } catch (IOException e) 
        {
            System.out.print("Disconect detected: ");

            if (peerId != null)
            {
                System.out.println("Peer " + peerId);
                Server.getMemory().conections.remove(peerId);
            }
            else
                System.out.println("Server @ " + socket.getInetAddress().getHostAddress());
            
                
        }
    }

    public String analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
        String peer, file;
        int newMemory;
        peer = "";

        switch (splitMessage[0].trim()) {
        case "Peer":
            int port = Integer.parseInt(splitMessage[3]);
            String peerAddr = splitMessage[2];

            peer = splitMessage[1];

            try {
                Server.getMemory().addConnection(peer, InetAddress.getByName(peerAddr), port);
            } catch (UnknownHostException e) {
                System.out.println("Couldn't find peer");
                break;
            }

            connected = true;

            System.out.println("New peer connected: Peer" + peer + "@" + peerAddr + ":" + port);

            this.peerId = peer;
            Server.getMemory().updatePeerMemory(peer, Integer.parseInt(splitMessage[4]));
            return "connected";

        case "SYNC":
            handleSync(message);
            return "end";

        case "BACKUP":

            if (!connected)
                return "";

            peer = splitMessage[2];
            int memory = Integer.parseInt(splitMessage[4]);
            return getAvailablePeers(peer, Integer.parseInt(splitMessage[3]), splitMessage[1],memory);

        case "RESTORE":
            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];
            return getPeersWithFile(file);

        case "CHUNK":
            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];

            System.out.println("BEGIN SPLIT MESSAGE IN SERVER LISTENER");
            for (int i = 0; i < splitMessage.length; i++) {
                System.out.println(splitMessage[i]);
            }
            System.out.println("END SPLIT MESSAGE IN SERVER LISTENER");
            // deleteChunks(peer, file);
            // Server.getMemory().updateMemory();
            break;

        case "STORED":
            if (!connected)
                return "";

            peer = splitMessage[1];
            String chunkID = splitMessage[2];
            String key = chunkID + "-" + peer;

            if (!Server.getMemory().serverSavedChunks.contains(key)){
                Server.getMemory().serverSavedChunks.add(key);
                Server.getMemory().updatePeerMemory(peer,Integer.parseInt(splitMessage[3]));
                Server.getMemory().updateMemory();
            }
            break;

        case "DELETE":

            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];
            Server.getMemory().removeServerFile(peerId, file);
            return getPeersWithFile(file);

        case "DELETED":

            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];
            deleteChunks(peer, file);
            Server.getMemory().updatePeerMemory(peer, Integer.parseInt(splitMessage[4]));
            Server.getMemory().updateMemory();
            break;

        case "REMOVED":
            peer = splitMessage[1];
            file = splitMessage[2];
            newMemory = Integer.parseInt(splitMessage[5]);
            int chunkNo = Integer.parseInt(splitMessage[3]);
            int repDegree = Integer.parseInt(splitMessage[4]);
            Server.getMemory().updatePeerMemory(peer, newMemory);
            return receiveRemoved(peer, file, chunkNo, repDegree);
        
            case "SAVED":
            peer = splitMessage[1];
            file = splitMessage[2];
            setInitiatorPeer(peer,file);
            break;

        case "MEMORY":
            newMemory =  Integer.parseInt(splitMessage[2]);
            peer = splitMessage[1];
            Server.getMemory().updatePeerMemory(peer, newMemory);
            System.out.println("Updated memory of peer " + peer);
            break;

        default:
            System.out.println("Unknown message: " + splitMessage[0].trim());
        }
        System.out.println("peer"+peer+Server.getMemory().peersMemory.get(peer));

        return "";
    }
    public void setInitiatorPeer(String peer, String file){

        if(Server.getMemory().backupInitiatorPeer.containsKey(peer)){
            if (!Server.getMemory().backupInitiatorPeer.get(peer).contains(file)){
                Server.getMemory().backupInitiatorPeer.get(peer).add(file);
            }
        } else {
            Server.getMemory().backupInitiatorPeer.put(peer,new ArrayList<String>());
            Server.getMemory().backupInitiatorPeer.get(peer).add(file);
        }

    }

    private String receiveRemoved(String peer, String file, int chunkNo, int repDegree) {

        String chunkId = file + "-" + chunkNo;
        String key = chunkId + "-" + peer;
        Server.getMemory().serverSavedChunks.remove(key);
        int count = 0;
        System.out.println(chunkId+"\n");
        for (int i = 0; i < Server.getMemory().serverSavedChunks.size(); i++) {
            String[] splitMessage = Server.getMemory().serverSavedChunks.get(i).split("-");
            String chunkId1 = splitMessage[0] + "-" + splitMessage[1];
            System.out.println(chunkId1);
            if (chunkId.equals(chunkId1)) {
                count++;
            }
        }
        int delta = repDegree - count;
        System.out.println(delta + " " + count + " " + repDegree);
         return ""+delta;
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

        if(conectionPorts.equals(""))
            conectionPorts = " ";

        return conectionPorts;
    }
    public static boolean isInitiator(String peer, String file){
        if (Server.getMemory().backupInitiatorPeer.containsKey(peer)){
            if (Server.getMemory().backupInitiatorPeer.get(peer).contains(file))
            return true;
        }
        return false;
    }

    public static String getAvailablePeers(String peer, int replicationDegree, String chunkId, int memory) {
        String sb = "";
        String file = chunkId.split("-")[0];

        for (String key : Server.getMemory().conections.keySet()) {
            
            if (replicationDegree > 0) {
                if (!isInitiator(key,file) && !key.equals(peer) && !Server.getMemory().serverSavedChunks.contains(chunkId + "-" + peer) && Server.getMemory().peersMemory.get(key) >= memory) {
                    sb += Server.getMemory().conections.get(key).getKey().getHostAddress() + "-"
                            + Server.getMemory().conections.get(key).getValue() + " ";
                    replicationDegree--;
                }
            } else
                break;

        }

        if (replicationDegree > 0)
            System.out.println("Warning: There aren't enough peers to meet replication demand");

        if(sb.equals(""))
            sb = " ";

        return sb;
    }

    public void handleSync(String syncMsg) {
        String[] msgParams = syncMsg.split(" ");

        if (msgParams.length != 3)
            return;

        long lUpdt = Long.parseLong(msgParams[2]);

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            if (lUpdt < Server.getMemory().getLastUpdated()) {
                oos.writeObject(Server.getMemory());
                System.out.println("Sent new memory");
            } else
                oos.writeObject(null);
        } catch (IOException e) {
            System.out.println("Couldn't send data to server");
            return;
        }

    }
}