package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

public class Memory implements Serializable
{
	public ArrayList<FileInfo> files = new ArrayList<FileInfo>(); //key: fileId-ChunkNo
	public ConcurrentHashMap<String,Chunk> savedChunks= new ConcurrentHashMap<String,Chunk>(); //key: fileId-ChunkNo String: fileId
	public HashMap<String,String> chunksToRestore= new HashMap<String,String>();
	public ConcurrentHashMap<String,Integer> savedOcurrences = new ConcurrentHashMap<String,Integer>();
	public HashMap<String, Pair<Integer, InetAddress>> confirmedChunks = new HashMap<String, Pair<Integer, InetAddress>>(); //chunkid < port, address>
	public transient ConcurrentHashMap<String, Pair<InetAddress, Integer>> conections = new ConcurrentHashMap<String, Pair<InetAddress, Integer>>();
	public ArrayList<String> serverSavedChunks = new ArrayList<String>();  //chunkId-peerId
	public ArrayList<String> deletedFiles= new ArrayList<String>();
	public int capacity = 999999999;
	public int memoryUsed = 0;
	public int availableCapacity= capacity - memoryUsed;
	private long lastUpdated = 0;
	private static final long serialVersionUID = 1L; //assign a long value
	
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

	public void updateMemory()
	{
		this.lastUpdated = System.currentTimeMillis();
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

	public void addConnection(String peerID, InetAddress address, int port)
	{
		conections.put(peerID, new Pair<InetAddress, Integer>(address, port));
	}

	public String getPeerPort(String peerID)
	{
		return conections.get(peerID).getKey().getHostAddress() + "-" + conections.get(peerID).getValue();
	}

	public String getPeerId(int peerPort, InetAddress peerAddress){
		for(String key : conections.keySet()){
			if (conections.get(key).getValue() == peerPort && conections.get(key).getKey().getHostAddress().equals(peerAddress.getHostAddress()))
			return key;
		}
		return "";
	}

	public void printServerSavedChunks()
	{
		for(String key: serverSavedChunks)
			System.out.println(key);
	}


	public long getLastUpdated()
	{
		return lastUpdated;
	}
}