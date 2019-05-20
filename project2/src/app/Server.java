package app;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import utils.Memory;
import utils.Pair;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.security.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.security.cert.X509Certificate;
import threads.listeners.ServerThread;

public class Server {
	private static SSLServerSocket serverSocket;
	public static ScheduledThreadPoolExecutor executor;
	private static Memory memory = new Memory();
	private static InetAddress tcp_addr;
	public static int tcp_port;
	private static ConcurrentHashMap<String, Pair<Integer, SSLSocket>> servers;

	public static void main(String args[]) 
	{
		if (args.length != 6) 
		{
			System.out.println("Wrong number of arguments\nUsage: Server <server1_addr> <server1_port> <server2_addr> <server2_port> <server3_addr> <server3_port>");
			return;
		}

		servers = new ConcurrentHashMap<String, Pair<Integer, SSLSocket>>();

		if(!checkStores()) 
		{
			System.out.println("Couldn't create local key/trust stores");
			return;
		}	

		try 
		{
			tcp_addr = InetAddress.getByName(args[0]);
			tcp_port = Integer.parseInt(args[1]);

			/*SSLSocket s1 = Peer.createSocket(InetAddress.getByName(args[2]), Integer.parseInt(args[3])),
				s2 = Peer.createSocket(InetAddress.getByName(args[4]), Integer.parseInt(args[5]));

			servers.put(args[2], new Pair<Integer, SSLSocket>(Integer.parseInt(args[3]), s1));
			servers.put(args[4], new Pair<Integer, SSLSocket>(Integer.parseInt(args[5]), s2));

			if(s1 != null)
				s1.startHandshake();

			if(s2 != null)
				s2.startHandshake();*/
		} 
		catch (Exception e) 
		{
			
		}

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			serverSocket = (SSLServerSocket) ssf.createServerSocket(tcp_port, 30, tcp_addr);

			serverSocket.setNeedClientAuth(true);
			serverSocket.setEnabledCipherSuites(new String[] 
			{ 
				"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_NULL_MD5",
				"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA"
			});
			serverSocket.setEnabledProtocols(new String[] {"TLSv1.2"});

			System.out.println("Server ready to receive connections...");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server - Failed to create SSLServerSocket");
			return;
		}

		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
		executor.execute(new ServerThread(serverSocket, executor));	
		executor.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run() 
			{
				startSync();
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	public static boolean setCertificateHandling()
	{
		TrustManager[] trustAllCerts = new TrustManager[] 
		{ 
			new X509TrustManager() 
			{     
				public java.security.cert.X509Certificate[] getAcceptedIssuers() 
				{ 
					return new X509Certificate[0];
				} 
				
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {} 
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
			} 
		}; 
		
		// Install the all-trusting trust manager
		try 
		{
			SSLContext sc = SSLContext.getInstance("SSL"); 
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} 
		catch (GeneralSecurityException e) 
		{
			System.out.println("Couldn't install new trust manager");
			return false;
		} 

		return true;
	}

	public static boolean checkStores() 
	{
		File store = new File("Server/keystore.jks");

		if(!store.exists())
		{
			System.out.println("Couldn't find server key store");

			return false;
		}

		store = new File("Server/truststore.jks");

		if(!store.exists())
		{
			System.out.println("Couldn't find server trust store");

			return false;
		}

		System.setProperty("javax.net.ssl.keyStore", "Server/keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("javax.net.ssl.trustStore", "Server/truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

		return true;
	}

	public static SSLServerSocket getServerSocket() {
		return serverSocket;
	}

	public static Memory getMemory() {
		return memory;
	}

	public static InetAddress getAddress()
	{
		return tcp_addr;
	}

	public static int getPort()
	{
		return tcp_port;
	}

	public static void setMemory(Memory newMemory)
	{
		ConcurrentHashMap<String, Pair<Socket, Integer>> connections = memory.conections;

		memory = newMemory;
		memory.conections = connections;
	}

	public static void startSync()
	{
		SSLSocket socket;
		Entry<String, Pair<Integer, SSLSocket>> entry;
		Iterator<Entry<String, Pair<Integer, SSLSocket>>> it = servers.entrySet().iterator();

		while(it.hasNext())
		{
			System.out.println("SYNC call");

			entry = it.next();
			socket = entry.getValue().getValue();

			if(socket == null)
			{
				try
				{
					socket = Peer.createSocket(InetAddress.getByName(entry.getKey()), entry.getValue().getKey());

					if(socket != null)
						socket.startHandshake(); 
				}
				catch(Exception e)
				{
					continue;
				}
			}

			try 
			{
				OutputStream ostream = socket.getOutputStream();
				PrintWriter pwrite = new PrintWriter(ostream, true);
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

				pwrite.println("SYNC " + Server.getAddress().getHostAddress() + " " + Server.getMemory().getLastUpdated());
                socket.setSoTimeout(5000);

				try
				{
					Memory newMemory = (Memory) ois.readObject();

					Server.setMemory(newMemory);

					System.out.println("Updated memory");
				}
				catch(Exception e)
				{
					if(!(e instanceof SocketTimeoutException))
						System.out.println("Couldn't save new memory");
				}
			} 
			catch (IOException e) 
			{
				System.out.println("Couldn't sync");
			}
		}
    }
}