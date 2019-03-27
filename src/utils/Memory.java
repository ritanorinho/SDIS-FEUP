package utils;


import java.util.ArrayList;
import java.util.HashMap;

public class Memory {
	public ArrayList<FileInfo> files= new ArrayList<FileInfo>();
	// String: fileId-ChunkNo Integer: Number of  occurrences
	public HashMap<String,Integer> backupChunks=new HashMap<String,Integer>();

	public boolean hasFile(String fileId){
		for(int i=0; i<files.size();i++){
			if(files.get(i).getFileId().equals(fileId))
				return true;
		}

		return false;
	}
	
}
