package project;

public  class Chunk {
	private static int MAX_SIZE = 64000;
	private int fileId;
	private int chunkNo;
	private byte[] data;
	private int length;
	
	
	public Chunk(int chunkNo,byte[] data, int length) {
		this.chunkNo = chunkNo;
		this.data=data;
		this.length=length;
	}
	


	public int getFileId() {
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



