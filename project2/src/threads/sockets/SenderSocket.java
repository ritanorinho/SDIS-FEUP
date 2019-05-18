package threads.sockets;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;

public class SenderSocket extends Thread {
    private SSLSocket socket;
    private byte[] message;

    public SenderSocket(SSLSocket socket, byte[] message) {
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