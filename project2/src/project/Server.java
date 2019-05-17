package project;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import utils.Memory;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.security.*;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			serverSocket = (SSLServerSocket) ssf.createServerSocket(tcp_port);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server - Failed to create SSLServerSocket");
			return;
		}

		// Require client authentication
		serverSocket.setNeedClientAuth(false); // TODO Change
		serverSocket.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" });
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
		executor.execute(new TCPThread(serverSocket, executor));

		/*
		 * if(!createStores()) return;
		 * 
		 * executor.execute(new TCPThread("start"));
		 */
	}

	public static boolean createStores() {
		char[] pwdArray = "password".toCharArray();
		KeyStore ks, ts;

		try {
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("keystore.jks"), pwdArray);
		} catch (Exception e) {
			try {
				ks = KeyStore.getInstance(KeyStore.getDefaultType());
				ks.load(null, pwdArray);

				try (FileOutputStream fos = new FileOutputStream("keystore.jks")) {
					ks.store(fos, pwdArray);
				}
			} catch (Exception e2) {
				System.out.println("Couldn't create keystore");
				e2.printStackTrace();
				return false;
			}
		}

		try {
			ts = KeyStore.getInstance("JKS");
			ts.load(new FileInputStream("truststore.jks"), pwdArray);
		} catch (Exception e) {
			try {
				ts = KeyStore.getInstance(KeyStore.getDefaultType());
				ts.load(null, pwdArray);

				try (FileOutputStream fos = new FileOutputStream("truststore.jks")) {
					ts.store(fos, pwdArray);
				}
			} catch (Exception e2) {
				System.out.println("Couldn't create trust store");
				e2.printStackTrace();
				return false;
			}
		}

		System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");

		System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

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