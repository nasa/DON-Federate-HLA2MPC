package gov.nasa.ksc.itacl.mpc;

import gov.nasa.ksc.itacl.mpc.models.Telemetry;
import gov.nasa.ksc.itacl.Utilities.*;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Vector;

public class MPCSocketServer extends Thread {
	
	public boolean IsRunning = false;
	private Vector<MPCSocketThread> clientThreads = new Vector<MPCSocketThread>();
	private Telemetry telemetry;
	private boolean isShutdown = false;
	private ServerSocket socket = null;
	private int port;
	
	public void SetPort(int port) {
		this.port = port;
	}
		
	public boolean isShutdown() {
		return isShutdown;
	}
	
	public void Listen(int port) {
		isShutdown = false;
		IsRunning = false;
		try {
			//create server socket and wait for a connection
			socket = new ServerSocket(port);
			Utils.info("MPC Socket Server Listening on " + socket.getInetAddress().getHostAddress() + ":" + String.valueOf(port) );
			Utils.info("waiting for clients...");
			IsRunning = true;
	
			while(!isShutdown){		
				Socket soc = socket.accept();
				registerClientAndStart(soc);
			}
		} 
		catch (IOException | SecurityException  | IllegalArgumentException | java.nio.channels.IllegalBlockingModeException ex) {
			Utils.error("Could not start MPC Socket Server.  " + ex.getMessage());
		}
		
	}
	
	public void registerClientAndStart( Socket socket ) {
		try {
			MPCSocketThread client = new MPCSocketThread(telemetry, socket.getOutputStream());
			Thread thread = new Thread(client);
			thread.start();
			clientThreads.add(client);
		} 
		catch (IllegalThreadStateException | IOException  e) {
			Utils.error("Could not start thread for a client. " + e.getMessage());
		}	
	}
	
	public MPCSocketServer(Telemetry telemetry) {
		this.telemetry = telemetry;
	}

	public void streamUpdate(Double time) {
		Iterator<MPCSocketThread> it = clientThreads.iterator();
		while(it.hasNext()){
			MPCSocketThread client = it.next();
			client.streamTime(time);
		}			
	}
	
	public void end(){
		try {
			int count = clientThreads.size();
			int i = 0;
		
			// make sure all clients to quit
			for(i = 0; i < count; ++i) {
				clientThreads.get(i).stop();
			}
		
			boolean isConnected = true;
			do {
				isConnected = false;
				for(i = 0; i < count; ++i) {
					if(clientThreads.get(i).isRunning()){
						isConnected = true;
						break;
					}
				}
				Thread.sleep(Utils.THREAD_SLEEP_TIME);
				Utils.info("Shutting down MPC clients");
			}
			while(isConnected);
			
			Utils.info("MPC clients have all disconnected. Shutting down MPC Streaming Server...");
			
			if(socket != null) { 
				socket.close();
			}
		} 
	
		catch (IOException | InterruptedException e) {
			Utils.error("Could not close the MPCserver socket\n" + e.getMessage());
		}
		
		Utils.info("MPCserver shutdown");
		isShutdown = true;
	}

	@Override
	public void run() {
		this.Listen(port);
	}
}
