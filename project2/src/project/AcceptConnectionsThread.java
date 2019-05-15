package project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import utils.Memory;

import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AcceptConnectionsThread extends Thread {
    private static Socket socket;
    private static ScheduledThreadPoolExecutor executor;
    private static Memory memory;

    public AcceptConnectionsThread(Socket socket, ScheduledThreadPoolExecutor executor, Memory memory) {
        this.socket = socket;
        this.executor = executor;
        this.memory = memory;

    }

    @Override
    public void run() {
        try {

            while (true) {
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
                    Server.getMemory().addConnection(peerID, socket, port, address);
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
                String peer = splitMessage[2];
                String otherPeer;
                if ((otherPeer = getOtherPeers(peer)) != null) {

                    try {
                        OutputStream outputStream;
                        DataOutputStream dataOutputStream;
                        outputStream = socket.getOutputStream();
                        dataOutputStream = new DataOutputStream(outputStream);
                        dataOutputStream.writeChars(otherPeer + "\n");
                        System.out.println("send message to peer " + otherPeer);
                    } catch (Exception e) {
                        System.out.println("Server error: it isn't possible to send a message to peer");
                    }
                }

                break;
            default:

            }
        }
        return false;
    }

    public static String getOtherPeers(String peer) {
        for (String key : memory.conectionsPorts.keySet()) {
            if (!key.equals(peer)) {
                return key;
            }

        }
        return null;
    }
}