package threads.scheduled;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import app.Server;

public class SaveMemoryTask implements Runnable {
    

    public SaveMemoryTask() {}

	@Override
    public void run() {
        FileOutputStream fileOut;
		try {
            fileOut = new FileOutputStream("Server/memory");
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(Server.getMemory());
            objectOut.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        

    }
}