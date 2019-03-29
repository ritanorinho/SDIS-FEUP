package project;

import java.io.UnsupportedEncodingException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
	void backup(String file, int repDegree) throws RemoteException, InterruptedException, UnsupportedEncodingException;
    void restore(String file) throws RemoteException;
    void delete(String file) throws RemoteException;
    void reclaim(int space) throws RemoteException;
    void state() throws RemoteException;

}
