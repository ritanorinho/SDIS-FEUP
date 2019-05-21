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
            System.out.println("Disconect detected");

            if(peerId != null)
                Server.getMemory().conections.remove(peerId);
        }
    }

    public String analizeMessage(String message) {

        String[] splitMessage = message.trim().split("\\s+");
        String peer, peerID, file;

        switch (splitMessage[0].trim()) {
        case "Peer":
            int port = Integer.parseInt(splitMessage[3]);
            String peerAddr = splitMessage[2];

            peerID = splitMessage[1];

            try
            {
                Server.getMemory().addConnection(peerID, InetAddress.getByName(peerAddr), port);
            }
            catch(UnknownHostException e)
            {
                System.out.println("Couldn't find peer");
                break;
            }
            
            connected = true;

            System.out.println("New peer connected: Peer" + peerID + "@" + peerAddr + ":" + port);

            this.peerId = peerID;

            return "connected";

        case "SYNC":
            handleSync(message);
            return "end";

        case "BACKUP":

            if (!connected)
                return "";

            peer = splitMessage[2];
            return getAvailablePeers(peer, Integer.parseInt(splitMessage[3]), splitMessage[1]);

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

            // System.out.println("Peer id " + peer);
            System.out.println("BEGIN SPLIT MESSAGE IN SERVER LISTENER");
            for (int i = 0; i < splitMessage.length; i++){
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
            System.out.println("Peer id " + peer);
            Server.getMemory().serverSavedChunks.add(key);
            Server.getMemory().updateMemory();
            break;

        case "DELETE":

            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];
            return getPeersWithFile(file);
            
        case "DELETED":

            if (!connected)
                return "";

            peer = splitMessage[2];
            file = splitMessage[1];
            deleteChunks(peer, file);
            Server.getMemory().updateMemory();
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

    private void restoreFile(String peer, String file){
        System.out.println("FILE IS RESTORED HERE");
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
                if (!key.equals(peer) && !Server.getMemory().serverSavedChunks.contains(chunckId + "-" + peer)) 
                { 
                    sb += Server.getMemory().conections.get(key).getKey().getHostAddress() + "-"
                            + Server.getMemory().conections.get(key).getValue() + " ";
                    replicationDegree--;
                }
            } else
                break;

        }

        if(replicationDegree > 0)
            System.out.println("Warning: There aren't enough peers to meet replication demand");

            
        return sb;
    }

	public void handleSync(String syncMsg)
	{
        String[] msgParams = syncMsg.split(" ");

        if(msgParams.length != 3)
            return;

        long lUpdt = Long.parseLong(msgParams[2]);

        try 
        {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            if(lUpdt < Server.getMemory().getLastUpdated())
            {
                oos.writeObject(Server.getMemory());
                System.out.println("Sent new memory");
            }
            else
                oos.writeObject(null);
        } 
        catch (IOException e) 
        {
            System.out.println("Couldn't send data to server");
            return;
        }
        
        
	}
}