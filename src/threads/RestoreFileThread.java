package threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import project.Peer;

public class RestoreFileThread implements Runnable {
	private String filename;
	private String fileId;

	public RestoreFileThread(String filename, String fileId) {
		this.filename= filename;
		this.fileId=fileId;
	}

	@Override
	public void run() {
		boolean aux =createFile();//System.out.println("NUMBER CHUNKS" +Peer.getMemory().requiredChunks);
		System.out.println("File: "+aux);
	}

	public boolean createFile() {
		String filename = "Peer"+Peer.getId() +"/"+"RESTORED"+"/"+this.filename;
		File finalFile= new File(filename);
		
		try {		
		
		if (!finalFile.exists()) {
			finalFile.getParentFile().mkdirs();
			finalFile.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(finalFile,true);
		ArrayList<String> sortedChunks = new ArrayList<String>(Peer.getMemory().requiredChunks.keySet());
		String[] split = sortedChunks.get(0).trim().split("-");
		if (sortedChunks.size() >1) {
		sortedChunks.add(split[0]+"-1");
		}
		sortedChunks.sort((o1, o2) -> {
            int chunk1 = Integer.valueOf(o1.split("-")[1]);
            int chunk2 = Integer.valueOf(o2.split("-")[1]);
            return Integer.compare(chunk1, chunk2);
        });
		
		
		for (String key: sortedChunks) {
			String[] splitChunkName= key.trim().split("-");
			String chunkPath =  "Peer"+Peer.getId() +"/"+"CHUNK"+"/"+splitChunkName[0]+"/"+splitChunkName[1];
			
			File chunkFile = new File(chunkPath);
			if (!chunkFile.exists()) {
				
				return false;
			}
			byte[] content = new byte[(int) chunkFile.length()];

			FileInputStream in = new FileInputStream(chunkFile);
			in.read(content);
			fos.write(content);
			Peer.getMemory().requiredChunks.remove(key);
		}
		fos.close();
		return true;
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return false;

	}
	}