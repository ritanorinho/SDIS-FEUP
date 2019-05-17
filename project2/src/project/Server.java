package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import utils.Memory;
import java.util.concurrent.Executors;
import java.security.*;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.security.cert.X509Certificate;

public class Server {
	private static SSLServerSocket serverSocket;
	public static ScheduledThreadPoolExecutor executor;
	private static final Memory memory = new Memory();
	private static InetAddress tcp_addr;
	public static int tcp_port;

	public static void main(String args[]) {
		System.out.println("main " + memory.conections.size());

		if (args.length != 2) {
			System.out.println("Wrong number of arguments\nUsage: Server <tcp_addr> <tcp_port>");
			return;
		}

		try {
			tcp_addr = InetAddress.getByName(args[0]);
			tcp_port = Integer.parseInt(args[1]);

		} catch (UnknownHostException e) {
			System.out.println("Couldn't find server socket host");
			return;
		}

		new File("Server").mkdirs();

		if(!createStores()) 
		{
			System.out.println("Couldn't create local key/trust stores");
			return;
		}	

		/*
		if(!setCertificateHandling())
			return; */

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			serverSocket = (SSLServerSocket) ssf.createServerSocket(tcp_port, 30, tcp_addr);

			serverSocket.setNeedClientAuth(false);
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
		executor.execute(new TCPThread(serverSocket, executor));	
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

	public static boolean createStores() {
		char[] pwdArray = "password".toCharArray();
		KeyStore ks;

		try {
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("Server/keystore.jks"), pwdArray);
		} catch (Exception e) {
			try {
				ks = KeyStore.getInstance(KeyStore.getDefaultType());
				ks.load(null, pwdArray);

				try (FileOutputStream fos = new FileOutputStream("Server/keystore.jks")) {
					ks.store(fos, pwdArray);
				}
			} catch (Exception e2) {
				System.out.println("Couldn't create keystore");
				e2.printStackTrace();
				return false;
			}
		}

		System.setProperty("javax.net.ssl.keyStore", "Server/keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");

		return true;
	}

	public static SSLServerSocket getServerSocket() {
		return serverSocket;
	}

	public static Memory getMemory() {
		System.out.println("Memory " + memory.conections.size());
		return memory;
	}

	public static String message() {
		return "abc";
	}

}