package utils;

import java.io.File;
import java.security.MessageDigest;

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

}
