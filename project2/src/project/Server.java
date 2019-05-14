package project;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import utils.Memory;
import java.util.concurrent.Executors;
import java.security.*;
import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.io.IOException;


public class Server
{
    private static SSLServerSocket serverSocket;
    private static ScheduledThreadPoolExecutor executor;
    private static Memory memory;
    private static InetAddress tcp_addr;
    private static int tcp_port;

    public static void main(String args[])
    {
        if(args.length != 2)
        {
            System.out.println("Wrong number of arguments\nUsage: Server <tcp_addr> <tcp_port>");
            return;
        }

        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);

        try
        {
            tcp_addr = Inet4Address.getByName(args[0]);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Couldn't find server socket host");
            return;
        }
        
        tcp_port = Integer.parseInt(args[1]);
 
        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  
        
        try 
        {  
            serverSocket = (SSLServerSocket) ssf.createServerSocket(tcp_port);  
        }  
        catch(IOException e) 
        {  
            System.out.println("Server - Failed to create SSLServerSocket");  
            e.getMessage();  
            return;  
        } 

         // Require client authentication  
        serverSocket.setNeedClientAuth(false);  

        serverSocket.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});

        /*
        try 
        {
            //Security.setProperty("ssl.ServerSocketFactory.provider", "oops");

            ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();

            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(tcp_port, 30, tcp_addr);

            

            SSLServerSocket sslListener = (SSLServerSocket) serverSocket;
            sslListener.setNeedClientAuth(true);
            sslListener.setEnabledCipherSuites(
            new String[] { "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256" });
            sslListener.setEnabledProtocols(
            new String[] { "TLSv1.2" });

            serverSocket.setNeedClientAuth(true);

        } 
        catch (Exception e) 
        {
            System.out.println("Couldn't create ssl server socket");
            e.printStackTrace();
            return;
        } 

        /*
        finally 
        {
            // restore the security properties
        
            if (reservedSSFacProvider == null)
                reservedSSFacProvider = "";
            
            Security.setProperty("ssl.ServerSocketFactory.provider", reservedSSFacProvider); 
        } */

    executor.execute(new TCPThread("start"));
}

private static ServerSocketFactory getServerSocketFactory(String type) 
{
    if (type.equals("TLS")) {
        SSLServerSocketFactory ssf = null;
        try {
            // set up key manager to do server authentication
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            char[] passphrase = "passphrase".toCharArray();

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream("testkeys"), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            ssf = ctx.getServerSocketFactory();
            return ssf;
        } catch (Exception e) {
            e.printStackTrace();
        }
    } else {
        return ServerSocketFactory.getDefault();
    }
    return null;
}


    public static SSLServerSocket getServerSocket()
    {
        return serverSocket;
    }

    public static Memory getMemory()
    {
        return memory;
    }

    

}