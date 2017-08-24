package gov.nasa.ksc.itacl.mpc.models;

public class Update {
	
	protected String name;
	protected String parentName;
	protected double [] position = new double[]{0,0,0};
	protected double [] rotation = new double[]{1,0,0,0};
	protected double [] scale = new double[]{1,1,1};
	protected double time;
	
	public Update() {name = parentName = ""; time = 0;}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setParentName(String parentName){
		this.parentName = parentName;
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
}
