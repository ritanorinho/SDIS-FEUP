package utils;

public  class Chunk {
	
	private String fileId;
	private int chunkNo;
	private byte[] data;
	private int length;
	private String chunkId;
	private int replicationDegree;
	
	
	public Chunk(String fileId,int chunkNo,byte[] data, int length, String chunkId,int replicationDegree) {
		this.chunkNo = chunkNo;
		this.data=data;
		this.length=length;
		this.fileId=fileId;
		this.chunkId =chunkId;
		this.replicationDegree=replicationDegree;
		
	}
	
	
	


	public String getFileId() {
		return fileId;
	}


	public int getChunkNo() {
		return chunkNo;
	}



	public byte[] getData() {
		return data;
	}


	public int getLength() {
		return length;
	}



	public String getChunkId() {
		// TODO Auto-generated method stub
		return this.chunkId;
	}




	public int getChunkSize() {
		// TODO Auto-generated method stub
		return this.length;
	}





	public int getReplicationDegree() {
		// TODO Auto-generated method stub
		return this.replicationDegree;
	}

	

}



