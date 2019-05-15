package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.net.ssl.SSLSocket;

public class ReceiveMessageFromServer extends Thread {
    SSLSocket socket;

    public ReceiveMessageFromServer(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        while (true) {
            BufferedReader d;
            try {
                System.out.println("abc");
                //d = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                System.out.println("---"+this.socket.getInputStream().read());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    }
    
}