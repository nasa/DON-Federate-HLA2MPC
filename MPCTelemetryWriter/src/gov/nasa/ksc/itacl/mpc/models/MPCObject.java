package gov.nasa.ksc.itacl.mpc.models;

import java.util.HashMap;
import java.util.Map;
import jat.coreNOSA.spacetime.CalDate;

public class MPCObject {

	private static final double MODIFIED_JULIAN = 40000;
	protected boolean visible = true;
	private boolean convertPosition = false;
	private boolean convertRotation = false;
	private boolean groundClamp = false;	

	protected double time = 0;
	protected double [] position = new double[]{0,0,0};
	protected double [] rotation = new double[]{0,0,0,1};
	protected double [] scale = new double[]{1,1,1};
	protected Map<String, String> metaData = new HashMap<>();
	private double heightAboveTerrain = -1;
	protected String name = null;
	protected String parentName = null;	
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setParentName(String name){
		parentName = name;
	}
	
	public void setTime(double time){
		this.time = time;
	}
	
	public void setPosition(double [] position){
		this.position = position;
	}
	
	public void setRotation(double [] rotation) {
		this.rotation = rotation;
	}
	
	public void setScale(double [] scale) {
		this.scale = scale;
	}
	
	public void setMetaData(String name, String value) {
		metaData.put(name, value);
	}
	
	public void setHeightAboveTerrain(double heightAboveTerrain) {
		this.heightAboveTerrain = heightAboveTerrain;
	}

	public void setConvertPosition(boolean convertPosition) {
		this.convertPosition = convertPosition;
	}

	public void setConvertRotation(boolean convertRotation) {
		this.convertRotation = convertRotation;
	}	
	
	public void setGroundClamp(boolean groundClamp) {
		this.groundClamp = groundClamp;
	}
	
	public String getName() {
		return name;
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public double[] getPosition() {
		return position;
	}

	public double[] getRotation() {
		return rotation;
	}

	public double[] getScale() {
		return scale;
	}
	
	public double getTime() {
		return time;
	}
	
	public String getPrettyTime()
	{
		CalDate date = new CalDate(time + MODIFIED_JULIAN);
		
		return date.month() + "/" + date.day() + "/" + date.year() + " " + date.hour() + ":" + date.min() + ":" + Math.round(date.sec());
	}
	
	public String getMetaData(String key) {
		return metaData.get(key);
	}
	
	public Map<String,String> getMetaData() {
		return metaData;
	}
	
	public double getHeightAboveTerrain() {
		return heightAboveTerrain;
	}

	public boolean getConvertPosition() {
		return convertPosition;
	}
	
	public boolean getGroundClamp() {
		return groundClamp;
	}
	
	public boolean getConvertRotation() {
		return convertRotation;
	}
	
	public boolean isVisible()
	{
		return visible;
	}

}
