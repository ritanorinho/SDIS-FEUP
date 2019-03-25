package utils;


import java.util.ArrayList;
import java.util.HashMap;

public class Memory {
	public ArrayList<FileInfo> files= new ArrayList<FileInfo>();
	// String: fileId-ChunkNo Integer: Number of  occurrences
	public HashMap<String,Integer> backupChunks=new HashMap<String,Integer>();
	
}
