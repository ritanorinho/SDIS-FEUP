package project;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPRestoreServer extends Thread {

    private int port;
    private Socket socket;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;

    TCPRestoreServer(Socket socket) {
    }

    @Override
    public void run() {
    }
}