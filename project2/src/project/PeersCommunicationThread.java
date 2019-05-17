package project;

import java.net.Socket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import sockets.ReceiverSocket;

public class PeersCommunicationThread extends Thread {
    private Socket socket;
    private ScheduledThreadPoolExecutor executor;

    public PeersCommunicationThread(Socket socket, ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
        this.socket = socket;

    }

    @Override
    public void run() {
        byte[] message = null;
        executor.execute(new ReceiverSocket(this.socket,message,executor));
       

    }

}