package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import project.Peer;

public class Utils {
	
	public static String createFileId(File file) {
		
		String fileId = file.getName()+"."+String.valueOf(file.lastModified());
		return sha256(fileId);

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
	
	public static byte[] getHeader(String type, double protocolVersion,int serverID,String fileId, int chunkNo,int repDegree) {
		
		byte[] byteHeader=null;;
		try {
			String header = type+" " + protocolVersion + " " + serverID + " " + fileId + " "
					+ chunkNo + " " + repDegree + "\r\n\r\n";
			
			byteHeader = header.getBytes("US-ASCII");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return byteHeader;
	}
	
	public static void savedOccurrencesFile() {
		try {
		String filename = "Peer"+Peer.getId() +"/"+"SAVED"+"/"+"savedOccurrences.txt";
		File savedOcurrencesfile= new File(filename);
		if (!savedOcurrencesfile.exists()) {
			savedOcurrencesfile.getParentFile().mkdirs();
		savedOcurrencesfile.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(savedOcurrencesfile);
		for(String key: Peer.getMemory().savedOcurrences.keySet()) {
		
				
				String content = key + " "+Peer.getMemory().savedOcurrences.get(key)+"\n";
				byte[] byteContent= content.getBytes();
				fos.write(byteContent);
			
		}
		fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
