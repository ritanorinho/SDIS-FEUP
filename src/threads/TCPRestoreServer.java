package threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import project.Peer;

public class TCPRestoreServer extends Thread {

    private ServerSocket socket;
    private Socket client;
    private int port;
    private String chunkid;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private InputStream inputStream;
    private DataInputStream dataInputStream;
    private byte[] message;

    TCPRestoreServer(int port, String chunkid, byte[] message) {
        this.port = port;
        this.chunkid = chunkid;
        this.message = message;
    }

    @Override
    public void run() {

        try {
            this.socket = new ServerSocket(port);
            this.socket.setSoTimeout(10000);

            client = socket.accept(); 

            System.out.println("accepted");


            outputStream = client.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.writeInt(message.length);
            
            System.out.println("sent");

            //this.socket.close();
            
        } catch (IOException e) {
            System.out.println("Time out on " + Peer.getId() + ", port " + this.port);

        }
    }
}