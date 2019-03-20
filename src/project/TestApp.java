package project;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class TestApp {
	
	public TestApp() {
		
	}
	public static void main(String[] args) throws RemoteException, NotBoundException{
		if (args.length > 4 || args.length < 2) {
			System.out.println("ERROR: TestApp parameters must be: <peer_ap> <operation> <opnd_1> <opnd_2> ");
			return;
		}
		String accessPoint = args[0];
		String operation = args[1];
		String fileName;
		int replicationDegree;
		int spaceDisk;
		if (args.length >=3)
		fileName= args[2];
		 Registry registry = LocateRegistry.getRegistry("localhost");
		 RMIInterface stub = (RMIInterface) registry.lookup(accessPoint);
		 
	
		switch (operation)
		{
		case "BACKUP":
			if (args.length !=4) {
				System.out.println("ERROR: TestApp parameters must be: <peer_ap> BACKUP <file> <replicationDegree> ");
				return;				
			}
			fileName=args[2];
			replicationDegree = Integer.parseInt(args[3]);
			stub.backup(fileName, replicationDegree);
			break;
		case "RESTORE":
			if (args.length != 3) {
				System.out.println("ERROR: TestApp parameters must be: <peer_ap> RESTORE <file> ");
				return;	
			}
			replicationDegree = 3;
			fileName= args[2];
			break;
		case "DELETE":
			if (args.length != 3) {
				System.out.println("ERROR: TestApp parameters must be: <peer_ap> DELETE <file> ");
				return;	
			}
			fileName= args[2];	
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
		default:
					
	}
		
		
	}
}
