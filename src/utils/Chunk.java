package utils;

public  class Chunk {
	
	private String fileId;
	private int chunkNo;
	private byte[] data;
	private int length;
	private String chunkId;
	
	
	public Chunk(String fileId,int chunkNo,byte[] data, int length, String chunkId) {
		this.chunkNo = chunkNo;
		this.data=data;
		this.length=length;
		this.fileId=fileId;
		this.chunkId =chunkId;
		
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

	

}



