package project;

import java.io.UnsupportedEncodingException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

	public TestApp() {
	}

	
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
    String fileName;
		
		if (args.length > 4 || args.length < 2) {
			System.out.println("ERROR: TestApp parameters must be: <peer_ap> <operation> <opnd_1> <opnd_2> ");
			return;
		}else if (args.length >= 3) 	fileName = args[2];

    String accessPoint = args[0];
		String operation = args[1];
		
		int replicationDegree;
    int spaceDisk;
    Registry registry;
    RMIInterface stub=null;

    System.setProperty("java.net.preferIPv4Stack", "true");

    registry = LocateRegistry.getRegistry("localhost");

    try{stub = (RMIInterface) registry.lookup(accessPoint);}
    catch(NotBoundException e){
      e.printStackTrace();
    }

		switch (operation) {
      case "BACKUP":
        if (args.length != 4) {
          System.out.println("ERROR: TestApp parameters must be: <peer_ap> BACKUP <file> <replicationDegree> ");
          return;
        }
        fileName = args[2];
        replicationDegree = Integer.parseInt(args[3]);
			try {
				stub.backup(fileName, replicationDegree);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        break;

      case "RESTORE":
        if (args.length != 3) {
          System.out.println("ERROR: TestApp parameters must be: <peer_ap> RESTORE <file> ");
          return;
        }
        
        replicationDegree = 3;
        fileName = args[2];
        stub.restore(fileName);
        break;

      case "DELETE":
        if (args.length != 3) {
          System.out.println("ERROR: TestApp parameters must be: <peer_ap> DELETE <file> ");
          return;
        }
        fileName = args[2];
        stub.delete(fileName);
        break;

      case "RECLAIM":
        if (args.length != 3) {
          System.out.println("ERROR: TestApp parameters must be: <peer_ap> RECLAIM <maximum space>");
          return;
        }
        spaceDisk = Integer.parseInt(args[2]);
        stub.reclaim(spaceDisk);
        break;

      case "STATE":
        if (args.length != 2) {
          System.out.println("ERROR: TestApp parameters must be: <peer_ap> STATE");
          return;
        }
        stub.state();
        break;
		}
	}
}
