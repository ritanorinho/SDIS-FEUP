package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

public class FileInfo {
	
	
	private static int MAX_SIZE = 64000;
	private String fileId;
	private ArrayList<Chunk> chunks = new ArrayList<Chunk>();
	
	private File file ;
	private String filename;
	private String filePath;
	private int replicationDegree;
	public FileInfo(File file, String filePath, int repDegree) {
		this.file=file;
		this.filePath=filePath;
		this.replicationDegree=repDegree;
		fileId();
		calculateNumberChunks();
		// TODO Auto-generated constructor stub
	}

	public void calculateNumberChunks(){
		byte[] content = new byte[MAX_SIZE];
		
		BufferedInputStream buf;
		try {
			buf = new BufferedInputStream(new FileInputStream (file));
			int chunksCount=0;
			int size=0;
			String chunkId;
			while((size=buf.read(content))>0) {
				chunksCount++;				
				byte[] body = Arrays.copyOf(content, size);
				chunkId = this.fileId+"-"+chunksCount;
				this.chunks.add(new Chunk (this.fileId,chunksCount,body,size,chunkId));
				content= new byte[MAX_SIZE];
			}
			
			if (this.file.length() %64000==0) {
				 chunkId = this.fileId+"-"+chunksCount;
				this.chunks.add(new Chunk(this.fileId,chunksCount,null,0,chunkId));
			}
			
			
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	
	}
	public ArrayList<Chunk> getChunks(){
		return this.chunks;
	}
	
	public void fileId() {
		String fileName= this.file.getName();
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

		public String getFilePath() {
			// TODO Auto-generated method stub
			return this.filePath;
		}

		public int getReplicationDegree() {
			// TODO Auto-generated method stub
			return this.replicationDegree;
		}

}
