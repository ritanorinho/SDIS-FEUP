package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;

public class FileInfo {
	
	
	private static int MAX_SIZE = 64000;
	private String fileId;
	private ArrayList<Chunk> chunks = new ArrayList<Chunk>();
	private File file ;
	private String filename;
	public FileInfo(File file) {
		this.file=file;
		fileId();
		// TODO Auto-generated constructor stub
	}

	public ArrayList<Chunk> getChunks(){
		byte[] content = new byte[MAX_SIZE];
		double fileLength = file.length();
		
		BufferedInputStream buf;
		try {
			buf = new BufferedInputStream(new FileInputStream (file));
			int chunksCount=0;
			while((buf.read(content))>0) {
				chunksCount++;
				this.chunks.add(new Chunk (this.fileId,chunksCount,content,content.length));
				content= new byte[MAX_SIZE];
			}
			if (this.file.length() %64000==0) {
				this.chunks.add(new Chunk(this.fileId,chunksCount,null,0));
			}
			
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return chunks;
	}
	
	public void fileId() {
		String fileName= this.file.getName();
		System.out.println(fileName);
		this.filename=fileName;
		String lastModified= String.valueOf(this.file.lastModified());
		String fileId = fileName + "."+lastModified;
		this.fileId = sha256(fileId);	
		
		
		
		
	}
	public String getFileId()
	{
		return this.fileId;
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

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

}
