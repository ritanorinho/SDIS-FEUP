package sockets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import threads.AnalizeMessageThread;

public class ReceiverSocket extends Thread {
    private Socket socket;
    private byte[] message;
    private ScheduledThreadPoolExecutor executor;

    public ReceiverSocket(Socket socket, byte[] message, ScheduledThreadPoolExecutor executor) {
        this.socket = socket;
        this.message = message;
        this.executor = executor;
    }

  

	@Override
    public void run() {

        try {

            InputStream inputStream = this.socket.getInputStream();
			DataInputStream dataInputStream = new DataInputStream(inputStream);
		    int size = dataInputStream.readInt();
			this.message = new byte[size];
            dataInputStream.read(this.message);
            System.out.println(size);
            executor.execute(new AnalizeMessageThread(message, socket));
	               
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}