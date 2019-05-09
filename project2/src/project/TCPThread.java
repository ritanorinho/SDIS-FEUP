package project;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPThread extends Thread {

    private ServerSocket socket;
    private Socket client;
    private int port;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private byte[] message;

    TCPThread(int port, String chunkid, byte[] message) {
        this.message = message;
        this.port = port;
    }

    @Override
    public void run() {


        try {
            this.socket = new ServerSocket(this.port);
            this.socket.setSoTimeout(10000);

            client = socket.accept(); 

            outputStream = client.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.writeInt(message.length);
            dataOutputStream.write(message);
            
        } catch (IOException e) {}
    }



}