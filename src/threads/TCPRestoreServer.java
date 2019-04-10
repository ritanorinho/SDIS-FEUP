package threads;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import project.Peer;

public class TCPRestoreServer extends Thread {

    private ServerSocket socket;
    private Socket client;
    private int port;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private byte[] message;
    private String confirmMsg;

    TCPRestoreServer(int port, String chunkid, byte[] message, String confirmMsg) {
        this.port = port;
        this.message = message;
        this.confirmMsg = confirmMsg;
    }

    @Override
    public void run() {

        try {
            this.socket = new ServerSocket(port);
            this.socket.setSoTimeout(10000);

            client = socket.accept(); 

            outputStream = client.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.writeInt(message.length);
            dataOutputStream.write(message);

            System.out.println(confirmMsg);

            this.socket.close();
            
        } catch (IOException e) {}
    }
}