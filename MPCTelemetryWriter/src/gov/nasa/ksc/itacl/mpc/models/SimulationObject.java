package gov.nasa.ksc.itacl.mpc.models;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class SimulationObject {
	private String name;
	private TreeMap<Double,Update> updates = new TreeMap<Double,Update>();

	
	public synchronized void setName(String name) {
		this.name = name;	
	}
	
	public synchronized String getName() {
		return name;
	}
	
	public SimulationObject(String name){
		this.name = name;
	}
	
	public synchronized void addUpdate(Double time, Update update) throws Exception {
		synchronized(this.updates) {
			this.updates.put(time, update);
		}
	}
	
	public synchronized Update getUpdateFor(Double time) throws Exception {
		if(this.updates.containsKey(time)) {
			return this.updates.get(time);
		}
		else {
			Entry<Double, Update> entry = this.updates.floorEntry(time);
			if(entry == null) throw new Exception("No previous time found object " + name + ". waiting for first update.");
			
			Update update = entry.getValue();
			if(update != null) addUpdate(time, update);
			return update;
		}	
	}

	public synchronized Set<Double> getTimes() {
		System.out.println("Getting time for " + name + " : thread " + Thread.currentThread().getId());
		Set<Double> times;
		synchronized (this.updates) {
			times = this.updates.keySet();
		}
		return times;
	}
}
