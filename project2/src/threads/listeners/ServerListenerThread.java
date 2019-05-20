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

import app.Peer;
import app.Server;

public class ServerListenerThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;
    private boolean connected = false;

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

                if (analize.equals("end")) {
                    ostream.close();
                    pwrite.close();
                    istream.close();
                    receiveRead.close();
                    return;
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

        switch (splitMessage[0].trim()) {
        case "Peer":
            int port = Integer.parseInt(splitMessage[2]);

            peerID = splitMessage[1];

            Server.getMemory().addConnection(peerID, socket, port);
            connected = true;

            System.out.println("New peer connected: Peer" + peerID + "@" + socket.getInetAddress() + ":" + port);

            return "connected";

        case "SYNC":
            handleSync(message);
            return "end";

        case "BACKUP":

            if (!connected)
                return "";

            peer = splitMessage[2];
            return getAvailablePeers(peer, Integer.parseInt(splitMessage[3]), splitMessage[1]);

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
        case "UNAVAILABLE":   
            try {

                int peerPort = Integer.parseInt(splitMessage[1]);
                InetAddress peerAddress = InetAddress.getByName(splitMessage[2]);
                String id = Server.getMemory().getPeerId(peerPort, peerAddress);
                System.out.println(id);
                if (id != ""){
                System.out.println("null id");
                Server.getMemory().peersAlive.remove(id);
                Server.getMemory().peersAlive.put(id,false);
                }
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
                if (!peerPorts.containsKey(peer) && Server.getMemory().peersAlive.get(peer)) {
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

	public void handleSync(String syncMsg)
	{
        String[] msgParams = syncMsg.split(" ");

        if(msgParams.length != 3)
            return;

        long lUpdt = Long.parseLong(msgParams[2]);

        try 
        {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            if(lUpdt < Peer.getMemory().getLastUpdated())
                oos.writeObject(Server.getMemory());

            oos.close();
        } 
        catch (IOException e) 
        {
            System.out.println("Couldn't send data to server");
            return;
        }
        
        
	}
}