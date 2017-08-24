package gov.nasa.ksc.itacl.HLAtoDON.Models;

import gov.nasa.ksc.itacl.Utilities.Utils;
import gov.nasa.ksc.itacl.hla.Encoders.RotationalDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TextDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TimeDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TranslationDecoder;
import gov.nasa.ksc.itacl.mpc.models.MPCObject;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class ExecutionConfiguration extends MPCObject implements UpdateHLA {

	public static TranslationDecoder translationDecoder;
	public static TextDecoder textDecoder;
	public static RotationalDecoder rotationalDecoder;
	public static TimeDecoder timeDecoder;
	
	//Execution Configuration attributes
	private static AttributeHandle attributeRFRootFrameNameHandle;
	private static AttributeHandle attributeRFTimeHandle;
	private static RTIambassador ambassador;
	
	
	public static ObjectClassHandle objectHandle;
	public static AttributeHandleSet attributeHandle;
	
	public ExecutionConfiguration() {}

	@Override
	public void setPosition( byte [] bytes ) {
		try {
			double [][] translation = translationDecoder.decode(bytes);
			position = translation[0];
			double [] velocity = translation[1];
			setMetaData("Velocity", "" + velocity[0] + " " + velocity[1] + " " + velocity[2]);
		} 
		catch (DecoderException e) {
			Utils.warn("Could not decode position for " + getName() +"\n Using defaults!");
			position = new double[]{0,0,0};
		}
	}
	
	@Override
	public void setRotation( byte [] bytes ) {
		try {
			rotation = rotationalDecoder.decode(bytes);
		} 
		catch (DecoderException e) {
			Utils.warn("Could not decode rotation for " + getName() +"\n Using defaults!");
			rotation = new double[]{0,0,0,1};
		}
	}
	
	@Override
	public void setScale( byte [] bytes ){
		try {
			throw new Exception("setScale not yet implemented");
		} 
		catch (Exception e) {
			Utils.warn("Could not decode scale for " + getName() +"\n Using defaults");
			scale = new double[]{1,1,1};
		}
	}
	
	@Override
	public void setParentName( byte [] bytes ) {
		try {
			parentName = textDecoder.decode(bytes);
		} 
		catch (DecoderException e) {
			Utils.warn("Could not decode name for " + getName() +"\n Using defaults");
			parentName = null;
		}
	}

	@Override
	public void setTime(byte [] bytes) {
		try{
			time = timeDecoder.decode(bytes);
		}
		catch( DecoderException e) {
			Utils.warn("Could not decode name for " + getName() +"\n Using defaults");	
		}	
	}

	@Override
	public double getTime() {
		return Math.round(time * 100000);
	}
	
	public static boolean subscribe() {
		try {
			ambassador.subscribeObjectClassAttributes(objectHandle, attributeHandle);
			return true;
		} 
		catch (AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
			Utils.error("Could not subscribe to Execution Configuration.\n" + e.getMessage());
			return false;
		}
	}

	public static boolean Initialize(RTIambassador Ambassador) {
		ambassador = Ambassador;
		
		//Execution Configuration Object
		try {
			
			objectHandle = ambassador.getObjectClassHandle("ExecutionConfiguration");			
		
			//Execution Configuration Object Attributes
			attributeRFRootFrameNameHandle = ambassador.getAttributeHandle(objectHandle, "root_frame_name");
			attributeRFTimeHandle = ambassador.getAttributeHandle(objectHandle, "scenario_time_epoch");
						
			//Attribute set for ExecutionConfiguration
			attributeHandle = ambassador.getAttributeHandleSetFactory().create();

			attributeHandle.add(attributeRFRootFrameNameHandle);
			attributeHandle.add(attributeRFTimeHandle);
			
			return true;
		
		} 
		catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidObjectClassHandle e) {
			Utils.error("Could not initialize Execution Configuration.\n" + e.getMessage());
			return false;
		}
		
	}

	@Override
	public void setAttributes(AttributeHandleValueMap theAttributes) {
		try {
			setParentName( theAttributes.get(attributeRFRootFrameNameHandle) );
		}
		catch(NullPointerException e) {}

		try {
			setTime( theAttributes.get(attributeRFTimeHandle));
		}
		catch(NullPointerException e) {}			
		
	}
	
}
