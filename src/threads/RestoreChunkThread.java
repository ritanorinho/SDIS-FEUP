package threads;

public class RestoreChunkThread implements Runnable {
	String message;

	public RestoreChunkThread(String msg) {
		this.message= msg;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		System.out.println("RESTORE THREAD " + this.message);
		// TODO Auto-generated method stub

	}

}
