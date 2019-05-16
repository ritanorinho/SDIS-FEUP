package project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeersCommunicationThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;

    public PeersCommunicationThread(Socket socket, ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
        this.socket = socket;

    }

    @Override
    public void run() {

        try {
         
            byte[] message;
            int size;
            InputStream inputStream = socket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            size = dataInputStream.readInt();
            message = new byte[size];
            dataInputStream.read(message,0,size);
            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}