package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import app.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
	
	public static SSLSocket createSocket(InetAddress address, int port, boolean sendUnavailable) {
		SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket;

		try {
			socket = (SSLSocket) ssf.createSocket(address, port);
		} catch (IOException e) 
		{
			if(sendUnavailable)
			{
				try {
					Peer.sendMessageToServer("UNAVAILABLE " + address.getHostAddress() + " " + port, Peer.getServerSocket());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			
			System.out.println("Failed to create SSLSocket");
			return null;
		}

		socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
			"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", 
			"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"});

		socket.setEnabledProtocols(new String[] { "TLSv1.2" });

		return socket;
	}
	
	public static String createFileId(Path file) {
		
		String fileId;
		try {
			fileId = file.getFileName().toString() + "." + String.valueOf(Files.getLastModifiedTime(file));
			return sha256(fileId);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean checkStores(String id, String prePath) {

		Path truststorePath = Paths.get(prePath + "truststore.jks");
		Path keystorePath = Paths.get(prePath + "keystore.jks");

		if(!Files.exists(keystorePath)){
			System.out.println("Couldn't find " + id + " key store");
			return false;
		}

		if(!Files.exists(truststorePath)){
			System.out.println("Couldn't find " + id + " trust store");
			return false;
		}

		System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");

		return true;
	}
	
	public static final String sha256(String str) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");

			byte[] hash = sha.digest(str.getBytes("UTF-8"));

			StringBuffer hexStringBuffer = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);

				if (hex.length() == 1)
					hexStringBuffer.append('0');

				hexStringBuffer.append(hex);
			}

			return hexStringBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static byte[] getHeader(String type,int serverID,String fileId, int chunkNo,int repDegree) {
		
		byte[] byteHeader=null;;
		try {
			String header = type+ " " + serverID + " " + fileId + " "
					+ chunkNo + " " + repDegree + " " + "\r\n\r\n";
			
			byteHeader = header.getBytes("US-ASCII");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return byteHeader;
	}


	public static String[] byteArrayToStringArray(byte[] array){
		String string = new String(array, 0, array.length);
		return string.trim().split("\\s+");
	}

	public static byte[] getBody(byte [] messageBytes) {
		int i;
		for (i =0; i< messageBytes.length-4;i++) {
			if (messageBytes[i] == 0xD && messageBytes[i+1]== 0xA && messageBytes[i+2]== 0xD && messageBytes[i+3]== 0xA) {
				break;
			}
		}
		
		byte[] body = Arrays.copyOfRange(messageBytes,i+4,messageBytes.length);		
		return body;
	}

	public static Memory loadMemory(String path) {
		Memory memory = new Memory();

		try {	
			FileInputStream fi =  new FileInputStream(new File(path));
			ObjectInputStream oi = new ObjectInputStream(fi);

			memory = (Memory) oi.readObject();
			memory.conections = new ConcurrentHashMap<String, Pair<InetAddress, Integer>>();
			System.out.println("Loaded memory successfully");
			oi.close();

		} catch (FileNotFoundException e) {
			System.out.println("No memory to load.");
		}catch (IOException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return memory;
	}
}