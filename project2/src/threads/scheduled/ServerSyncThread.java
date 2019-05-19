package threads.scheduled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocket;

import app.Peer;
import utils.Pair;
import app.Server;

public class ServerSyncThread extends Thread
{
	private ConcurrentHashMap<String, Pair<Integer, SSLSocket>> servers;
    
    public ServerSyncThread(ConcurrentHashMap<String, Pair<Integer, SSLSocket>> servers)
    {
        this.servers = servers;
    }

    @Override
    public void run()
    {
		SSLSocket socket;
		Entry<String, Pair<Integer, SSLSocket>> entry;
		Iterator<Entry<String, Pair<Integer, SSLSocket>>> it = servers.entrySet().iterator();

		while(it.hasNext())
		{
			entry = it.next();
			socket = entry.getValue().getValue();

			if(socket == null)
			{
				try
				{
					//TODO Check if is altered on Server
					socket = Peer.createSocket(InetAddress.getByName(entry.getKey()), entry.getValue().getKey());

					if(socket != null)
						socket.startHandshake(); 
					
				}
				catch(Exception e)
				{
					continue;
				}
			}

			try 
			{
				OutputStream ostream = socket.getOutputStream();
				PrintWriter pwrite = new PrintWriter(ostream, true);
				InputStream istream = socket.getInputStream();
				BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

				pwrite.println("SYNC " + Server.getAddress().getHostAddress() + " " + Server.getMemory().getLastUpdated());

                socket.setSoTimeout(5000);

                //Receive memory
			} 
			catch (IOException e) 
			{
				System.out.println("Couldn't sync");
			}
			 
		}
    }
}