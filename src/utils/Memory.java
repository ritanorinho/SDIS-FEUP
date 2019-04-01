package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Memory {
	public ArrayList<FileInfo> files = new ArrayList<FileInfo>();
	public HashMap<String,String> filenameId= new HashMap<String,String>();
	// String: fileId-ChunkNo Integer: Number of occurrences
	public ConcurrentHashMap<String, Integer> backupChunks = new ConcurrentHashMap<String, Integer>();
	//String: fileId-ChunkNo
	public HashMap<String,Chunk> savedChunks= new HashMap<String,Chunk>();
	//String fileId-ChunkNo fileId
	public HashMap<String,String> requiredChunks= new HashMap<String, String>();
	public HashMap<String,Integer> restoredChunks= new HashMap<String, Integer>();
	public ConcurrentHashMap<String,Integer> savedOcurrences = new ConcurrentHashMap<String,Integer>();
	public boolean hasFile(String fileId) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getFileId().equals(fileId))
				return true;
		}

		return false;
	}
	

	public void removeChunks(String fileId) {

		for(int i=0; i<files.size();i++){
			if (files.get(i).getFileId().equals(fileId))
				files.remove(i);
		}

		for (Entry<String, Integer> entry : backupChunks.entrySet()) {
		   if(entry.getKey().split("-")[0].equals(fileId)){
			   backupChunks.remove(entry.getKey());
		   }
		}
	}
	
}
