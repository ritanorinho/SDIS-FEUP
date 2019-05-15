package project;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.PrintWriter;
import javax.net.ssl.SSLServerSocket;
import java.util.Collections;

public class TCPThread extends Thread {
    private SSLServerSocket serverSocket;
    private String message;

    TCPThread(String message) {
        this.message = message;
        System.out.println("tcp thread");
        serverSocket = Server.getServerSocket();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream;
                DataInputStream dataInputStream;
                inputStream = socket.getInputStream();
                dataInputStream = new DataInputStream(inputStream);
                
                System.out.println("Connected");
                String peerID = "Peer1";
                Server.getMemory().addConnection(peerID, socket);

                BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                        socket.getInputStream()
                    )
                );
                String s = in.readLine();
                
                System.out.println(s);


                // System.out.println("List of Peers Connected: ");
                // System.out.println(Collections.singletonList(Server.getMemory().conections));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}