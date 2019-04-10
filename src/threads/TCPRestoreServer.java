package threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPRestoreServer extends Thread {

    private ServerSocket socket;
    private Socket client;
    private int port;
    private String chunkid;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private InputStream inputStream;
    private DataInputStream dataInputStream;

    TCPRestoreServer(int port, String chunkid) {
        this.port = port;
        this.chunkid = chunkid;
        System.out.println(port);
    }

    @Override
    public void run() {
        System.out.println("running");

        try {
            this.socket = new ServerSocket(port);
            //socket.setSoTimeout(10000);

            System.out.println("going to established");

            client = socket.accept();

            System.out.println("connection established");

            outputStream = client.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            
            this.socket.close();

            System.out.println("closed connection");

            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}