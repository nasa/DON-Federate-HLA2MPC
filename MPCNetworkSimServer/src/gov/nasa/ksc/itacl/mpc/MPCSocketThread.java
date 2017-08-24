package gov.nasa.ksc.itacl.mpc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import gov.nasa.ksc.itacl.Utilities.Utils;
import gov.nasa.ksc.itacl.mpc.models.MPCObject;
import gov.nasa.ksc.itacl.mpc.models.Telemetry;

public class MPCSocketThread implements Runnable {

	private boolean shouldRun = true;
	private boolean isRunning = true;
	private Telemetry telemetry = null;
	private MPCWriter writer = null;
	private Double time = 0.0;
	private Double previousTime = 0.0;
	

	public MPCSocketThread(Telemetry telemetry, OutputStream stream) {
		this.writer = new MPCWriter(stream);
		this.telemetry = telemetry;
	}
	
	@Override
	public void run() {
		shouldRun = true;
		isRunning = true;
		
		if(!writeStart()) {
			return;
		}
		
		writeMissionMetadata();

		while(shouldRun) {
			
			double time = getTime();
			
			if(time > previousTime) {
				Map<Double, Map<String, MPCObject>> updates = telemetry.getRange(previousTime, true, time, false);
				Iterator<Entry<Double, Map<String, MPCObject>> > it = updates.entrySet().iterator();
				while(it.hasNext()) {
					Entry<Double, Map<String, MPCObject>> entry = it.next();
					try {
						writer.createTime(entry.getKey(), entry.getValue().values());
					} 
					catch (XMLStreamException e) {
						Utils.error(Thread.currentThread().getName() + " Could not stream time " + entry.getKey() + ".\n" +e.getMessage());
						shouldRun = false;
					}
				}				
				previousTime = time;
			}
			
			try {
				Thread.sleep(Utils.THREAD_SLEEP_TIME);
			} 
			catch (InterruptedException e) {
				Utils.warn("socket thread sleep was interrupted. " + e.getMessage());
			}	
		}	
		
		writeEnd();
		
		close();
		writer = null;
		telemetry = null;
		isRunning = false;
	}
	
	private void writeMissionMetadata() {		
		Map<String, String> metadata = telemetry.getMissionMetadata();
		Iterator<Entry<String, String>> it = metadata.entrySet().iterator();
		
		while(it.hasNext())
		{
			Entry<String,String> entry = it.next();
			try {
				writer.createMetaData(entry.getKey(), entry.getValue());
			}
			catch (XMLStreamException e) {
				Utils.warn("Could not write mission meta data for " + entry.getKey());
			}
		}		
	}

	private double getTime() {
		double time = 0;
		synchronized (this.time) {
			time = this.time;
		}
		return time;
	}

	public void streamTime(double time) {
		synchronized (this.time) {
			this.time = time;
		}
	}
	
	public void stop() {
		shouldRun = false;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	private boolean writeStart() {
		try {
			writer.createStartDocument();
			return true;
		} 
		catch (XMLStreamException e) {
			Utils.error("Could not start the MPC stream.\n" + e.getMessage());
			return false;
		}
	}
		
	private boolean writeEnd() {
		try {
			writer.createEndDocument();
			return true;
		} 
		catch (XMLStreamException e) {
			Utils.error("Could not end the MPC document. \n" + e.getMessage());
			return false;
		}
	}
	
	private boolean close() {
		try {
			writer.close();
			return true;
		} 
		catch (IOException e) {
			Utils.error("Could not close the mpc writer.\n" + e.getMessage());
			return false;
		}
	}	
}
