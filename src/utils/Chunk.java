package utils;

public  class Chunk {
	
	private String fileId;
	private int chunkNo;
	private byte[] data;
	private int length;
	
	
	public Chunk(String fileId,int chunkNo,byte[] data, int length) {
		this.chunkNo = chunkNo;
		this.data=data;
		this.length=length;
		this.fileId=fileId;
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

	

}



