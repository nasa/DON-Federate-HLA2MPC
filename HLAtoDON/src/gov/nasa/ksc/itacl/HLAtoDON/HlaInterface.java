package gov.nasa.ksc.itacl.HLAtoDON;

import gov.nasa.ksc.itacl.Utilities.FederationConfig;
import gov.nasa.ksc.itacl.mpc.models.Telemetry;

public interface HlaInterface {

	public static class Factory{
		public static HlaInterface HlaAmbassador() {
			return new HlaAmbassador();
		}
	}
	
	/*
	 * TODO
	 */
	/*void stop();*/
	boolean start(FederationConfig config, Telemetry telemetry);
	boolean disconnect();
	boolean isConnected();
	void advanceTime();
	boolean isShutdown();
	double getDoubleTime();
	double getPreviousTime();
	boolean advanceToGALT();
	boolean subscribe();
	boolean publish();
	boolean initHandles();
	boolean enableTimeConstrained();
	boolean disableTimeConstrained();
	boolean enableAsynchonousDelivery();
	boolean resignFederationExecution();
}
