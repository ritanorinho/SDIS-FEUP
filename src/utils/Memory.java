package utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.javafx.collections.MappingChange.Map;

import javafx.util.Pair;

public class Memory {
	public ArrayList<FileInfo> files = new ArrayList<FileInfo>();
	//key: fileId-ChunkNo
	public HashMap<String,Chunk> savedChunks= new HashMap<String,Chunk>();
	//key: fileId-ChunkNo String: fileId
	public HashMap<String,String> chunksToRestore= new HashMap<String,String>();
	public ConcurrentHashMap<String,Integer> savedOcurrences = new ConcurrentHashMap<String,Integer>();
	public HashMap<String, Pair<Integer,InetAddress>> confirmedChunks = new HashMap<String, Pair<Integer,InetAddress>>(); //chunkid < port, address>


	
	public int capacity = 999999999;
	public int memoryUsed = 0;
	public int availableCapacity= capacity - memoryUsed;
	
	
	public boolean hasFileByID(String fileId) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getFileId().equals(fileId))
				return true;
		}

		return false;
	}

	public boolean hasFileByName(String filename) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getFilename().equals(filename))
				return true;
		}

		return false;
	}

	public void removeChunks(String fileId) {

		for(int i=0; i<files.size();i++){
			if (files.get(i).getFileId().equals(fileId))
				files.remove(i);
		}

		for (Entry<String, Integer> entry : savedOcurrences.entrySet()) {
		   if(entry.getKey().split("-")[0].equals(fileId)){
			   savedOcurrences.remove(entry.getKey());
		   }
		}

		for (Entry<String, Chunk> entry : savedChunks.entrySet()) {
			if(entry.getKey().split("-")[0].equals(fileId)){
				savedChunks.remove(entry.getKey());
			}
		 }

	}
	
	public void updateMemoryUsed(int memory) {
		this.memoryUsed+=memory; 
	}
	public int getAvailableCapacity() {
		return this.capacity-this.memoryUsed;
	}
	
	public int getUsedMemory() {
		int usedMemory=0;
		for (String key: savedChunks.keySet()) {
			usedMemory+=savedChunks.get(key).getChunkSize();
			
		}
		this.memoryUsed=usedMemory;
		this.availableCapacity= this.capacity-usedMemory;
		return usedMemory;
	}
}
