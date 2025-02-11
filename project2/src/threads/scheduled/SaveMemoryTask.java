package threads.scheduled;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import app.Peer;
import app.Server;

public class SaveMemoryTask implements Runnable {
    
    private String path;
    private String actor;

    public SaveMemoryTask(String path, String actor) {
        this.path = path;
        this.actor = actor;
    }

	@Override
    public void run() {
        FileOutputStream fileOut;
        
		try {
            if(!Files.exists(Paths.get(this.path))){
                Files.createDirectories(Paths.get(this.path).getParent());
                Files.createFile(Paths.get(this.path));
            }
            
            fileOut = new FileOutputStream(this.path);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);

            if(this.actor.equals("server"))
                objectOut.writeObject(Server.getMemory());
            else if(this.actor.equals("peer"))
                objectOut.writeObject(Peer.getMemory());
            objectOut.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        

    }
}