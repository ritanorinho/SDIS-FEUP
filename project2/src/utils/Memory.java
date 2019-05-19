package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.net.Socket;


public class Memory {
	public ArrayList<FileInfo> files = new ArrayList<FileInfo>(); //key: fileId-ChunkNo
	public ConcurrentHashMap<String,Chunk> savedChunks= new ConcurrentHashMap<String,Chunk>(); //key: fileId-ChunkNo String: fileId
	public HashMap<String,String> chunksToRestore= new HashMap<String,String>();
	public ConcurrentHashMap<String,Integer> savedOcurrences = new ConcurrentHashMap<String,Integer>();
	public HashMap<String, Pair> confirmedChunks = new HashMap<String, Pair>(); //chunkid < port, address>
	public ConcurrentHashMap<String, Socket> conections = new ConcurrentHashMap<String, Socket>();
	public ConcurrentHashMap<String,String> conectionsPorts = new ConcurrentHashMap<String, String>();
	public ArrayList<String> serverSavedChunks = new ArrayList<String>();  //chunkId-peerId
	public ArrayList<String> deletedFiles= new ArrayList<String>();
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
			   if (!deletedFiles.contains(fileId)) deletedFiles.add(fileId);
			   savedOcurrences.remove(entry.getKey());
			   Utils.savedOccurrencesFile();
		   }
		}		
		for(Entry<String, Chunk> entry : savedChunks.entrySet())  {
			if(entry.getKey().split("-")[0].equals(fileId)){
				savedChunks.remove(entry.getKey());
			}
		 }
		
	}
	
	public void updateMemoryUsed(int memory) {
		this.memoryUsed+=memory; 
	}
	public int getAvailableCapacity() {
		return this.capacity-getUsedMemory();
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

	public void addConnection(String peerID, Socket socket, int port, String address){
		conections.put(peerID, socket);
		String portAddress = port + "-"+address;
		conectionsPorts.put(peerID,portAddress);

	}
}