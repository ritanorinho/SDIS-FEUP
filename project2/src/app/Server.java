package app;

import java.net.InetAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import utils.*;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import threads.listeners.ServerThread;
import threads.scheduled.SaveMemoryTask;

public class Server {
	private static SSLServerSocket serverSocket;
	public static ScheduledThreadPoolExecutor executor;
	private static Memory memory;
	private static InetAddress tcp_addr;
	public static int tcp_port;
	private static ConcurrentHashMap<String, Pair<Integer, SSLSocket>> servers;

	public static void main(String args[]) {
		
		if (args.length < 1) {
			System.out.println(
					"Wrong number of arguments\nUsage: Server server_port (<server_addr> <server_port>)*");
			return;
		} 

		servers = new ConcurrentHashMap<String, Pair<Integer, SSLSocket>>();

		try 
		{
			tcp_addr = InetAddress.getLocalHost();
			tcp_port = Integer.parseInt(args[0]);
		}
		catch (Exception e) 
		{
			System.out.println("Couldn't find this server's host");
			e.printStackTrace();
			return;
		}

		if (!Utils.checkStores("server", "Server/")) {
			System.out.println("Couldn't create local key/trust stores");
			return;
		}

		memory = Utils.loadMemory("Server/memory");

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			serverSocket = (SSLServerSocket) ssf.createServerSocket(tcp_port);

			serverSocket.setNeedClientAuth(true);
			serverSocket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_NULL_MD5", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
					"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA" });
			serverSocket.setEnabledProtocols(new String[] { "TLSv1.2" });

			System.out.println("Server ready to receive connections...");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server - Failed to create SSLServerSocket");
			return;
		}

		try
		{
			for(int i = 1; i < args.length - 1; i += 2)
			{
				InetAddress sa = InetAddress.getByName(args[i]);
				int sp = Integer.parseInt(args[i + 1]);
				SSLSocket socket = Utils.createSocket(sa, sp, false);

				if(socket == null)
					System.out.println("Couldn't connect to server, ignoring...");
				else
					socket.startHandshake();
				
				servers.put(args[i] + "-" + args[i + 1], new Pair<Integer, SSLSocket>(Integer.parseInt(args[i + 1]), socket));
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal error, aborting...");
			e.printStackTrace();
		}
		

		SaveMemoryTask saveMemory = new SaveMemoryTask("Server/memory", "server");
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
		executor.scheduleAtFixedRate(saveMemory, 5, 5, TimeUnit.SECONDS);

		executor.execute(new ServerThread(serverSocket, executor));
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				startSync();
			}
		}, 5, 5, TimeUnit.SECONDS);
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

	public static ConcurrentHashMap<String, Pair<Integer, SSLSocket>> getServers()
	{
		return servers;
	}

	public static void setMemory(Memory newMemory)
	{
		memory = newMemory;
	}

	public static void startSync()
	{
		SSLSocket socket;
		Entry<String, Pair<Integer, SSLSocket>> entry;
		Iterator<Entry<String, Pair<Integer, SSLSocket>>> it = servers.entrySet().iterator();

		//System.out.println("SYNC initiated with " + servers.size() + " servers");

		while(it.hasNext())
		{
			entry = it.next();
			socket = entry.getValue().getValue();

			if(socket == null)
			{
				try
				{
					socket = Utils.createSocket(InetAddress.getByName(entry.getKey().split("-")[0]), 
						entry.getValue().getKey(), false);

					if(socket != null)
						socket.startHandshake();
					else
						continue; 
				}
				catch(Exception e)
				{
					System.out.println("Error in server socket created");
					continue;
				}
			}

			try 
			{
				OutputStream ostream = socket.getOutputStream();
				PrintWriter pwrite = new PrintWriter(ostream, true);
				
				pwrite.println("SYNC " + Server.getAddress().getHostAddress() + " " + Server.getMemory().getLastUpdated());
				
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				
				Memory newMemory = (Memory) ois.readObject();

				if (newMemory == null) {
					//System.out.print("Up-to-date: ");
					//memory.printLastUpdatedDate();
					continue;
				}

				Server.setMemory(newMemory);

				System.out.print("Updated memory: ");
				memory.printLastUpdatedDate();
			} 
			catch (Exception e) 
			{
				System.out.println("Couldn't sync");

				if(e instanceof IOException)
				{
					servers.put(entry.getKey(), new Pair<Integer, SSLSocket>(entry.getValue().getKey(), null));
				}
		
			}
		}
		
	}
}