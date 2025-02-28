package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
				this.chunks.add(new Chunk (this.fileId,chunksCount,body,size,chunkId,this.replicationDegree));
				content= new byte[MAX_SIZE];
				
			}
			
			if (this.file.length() %64000==0) {
				 chunkId = this.fileId+"-"+chunksCount;
				this.chunks.add(new Chunk(this.fileId,chunksCount,null,0,chunkId,this.replicationDegree));
			}
			
			
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	
	}
	
	public void fileId() {
		this.filename = this.file.getName();
		this.fileId = Utils.createFileId(file);	
	}

	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) { 
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		
		folder.delete();
		
	}
	
	public ArrayList<Chunk> getChunks(){ return this.chunks; }

	public String getFileId() { return this.fileId;}

	public String getFilename() { return filename; }

	public void setFilename(String filename) { this.filename = filename; }

	public String getFilePath() { return this.filePath; }

	public int getReplicationDegree() { return this.replicationDegree; }

}
