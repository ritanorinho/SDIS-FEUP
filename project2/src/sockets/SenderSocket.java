package sockets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SenderSocket extends Thread {
    private Socket socket;
    private byte[] message;

    public SenderSocket(Socket socket, byte[] message) {
        this.socket = socket;
        this.message = message;

    }

    @Override
    public void run() {

        try {
            OutputStream outputStream = socket.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeInt(this.message.length);
			dataOutputStream.write(this.message);                      

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}