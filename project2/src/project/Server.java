package project;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import utils.Memory;

import java.util.concurrent.Executors;

public class Server  
{
    private static ServerSocket serverSocket;
    private static ScheduledThreadPoolExecutor executor;
    private static Memory memory;

    public static void main(String args[])
    {
        if(args.length != 2)
        {
            System.out.println("Wrong number of arguments\nUsage: Server <tcp_addr> <tcp_port>");
            return;
        }

        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

        InetAddress tcp_addr;

        try
        {
            tcp_addr = Inet4Address.getByName(args[0]);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Couldn't find server socket host");
            return;
        }
        
        int tcp_port = Integer.parseInt(args[1]);

        try
        {
            serverSocket = new ServerSocket(tcp_port, 30, tcp_addr); //30?
        }
        catch(IOException e)
        {
            System.out.println("Couldn't open server socket");
            return;
        }

        executor.execute(new TCPThread(port, chunkid, message));
    }

    public static ServerSocket getServerSocket()
    {
        return serverSocket;
    }

    public static Memory getMemory()
    {
        return memory;
    }
}