package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Memory {
	public ArrayList<FileInfo> files = new ArrayList<FileInfo>();
	// String: fileId-ChunkNo Integer: Number of occurrences
	public HashMap<String, Integer> backupChunks = new HashMap<String, Integer>();

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
