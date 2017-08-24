package gov.nasa.ksc.itacl.HLAtoDON.Models;

import java.util.logging.Level;
import java.util.logging.Logger;


import org.siso.spacefom.frame.SpaceTimeCoordinateState;

import ReferenceFrame.coder.SpaceTimeCoordinateStateCoder;
import gov.nasa.ksc.itacl.Utilities.Utils;
import gov.nasa.ksc.itacl.hla.Encoders.AttitudeDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.PositionDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TextDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TimeDecoder;
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

public class PhysicalEntity extends MPCObject implements UpdateHLA {

    public static TextDecoder textDecoder;
    public static PositionDecoder positionDecoder;
    public static AttitudeDecoder attitudeDecoder;
    public static TimeDecoder timeDecoder;
    public static SpaceTimeCoordinateStateCoder spaceTimeCoordStateDecoder;

    public static ObjectClassHandle objectHandle;
    public static AttributeHandleSet attributeHandle;

    private static RTIambassador ambassador;

    private static AttributeHandle attributePENameHandle;
    private static AttributeHandle attributePEParentHandle;   
    private static AttributeHandle attributePEStateHandle;
    private static AttributeHandle attributePEMultiStateHandle;

    public PhysicalEntity() {
    }

    @Override
    public void setParentName(byte[] bytes) {
        try {
            parentName = textDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode name for " + getName() + "\n Using defaults");
            parentName = null;
        }
    }

    public void setTime(byte[] bytes) {
        try {
            time = timeDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode name for " + getName() + "\n Using defaults");
        }
    }

    @Override
    public void setPosition(byte[] bytes) {
        try {
            position = positionDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode position for " + getName() + "\n Using defaults!");
            position = new double[]{0, 0, 0};
        }
    }

    @Override
    public void setRotation(byte[] bytes) {
        try {
            rotation = attitudeDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode rotation for " + getName() + "\n Using defaults!");
            rotation = new double[]{0, 0, 0, 1};
        }
    }

    @Override
    public void setScale(byte[] bytes) {
        Utils.warn("Could not decode scale for " + getName() + "\n Using defaults");
        scale = new double[]{1, 1, 1};
    }

    public void setState(byte[] bytes) {
        try {
            String state = textDecoder.decode(bytes);
            if (state.equalsIgnoreCase("on")) {
                visible = true;
            } else if (state.equalsIgnoreCase("off")) {
                visible = false;
            }
        } catch (DecoderException e) {
            Utils.warn("Could not decode state for " + getName() + "\n Using defaults!");
            visible = true;
        }
    }

    @Override
    public double getTime() {
        return Math.round(time * 100000);
    }

    public static boolean Initialize(RTIambassador RTIambassador) {
        ambassador = RTIambassador;

        try {
            //PhysicalEntity Object
            objectHandle = ambassador.getObjectClassHandle("PhysicalEntity");

            //PhysicalEntity Object Attributes
            attributePENameHandle = ambassador.getAttributeHandle(objectHandle, "name");

            attributePEParentHandle = ambassador.getAttributeHandle(objectHandle, "parent_reference_frame");

            attributePEStateHandle = ambassador.getAttributeHandle(objectHandle, "status");

            attributePEMultiStateHandle = ambassador.getAttributeHandle(objectHandle, "state");

            //Attribute set for PhysicalEntity
            attributeHandle = ambassador.getAttributeHandleSetFactory().create();
            attributeHandle.add(attributePENameHandle);
            attributeHandle.add(attributePEParentHandle);
            attributeHandle.add(attributePEStateHandle);
            attributeHandle.add(attributePEMultiStateHandle);

            return true;
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidObjectClassHandle e) {
            Utils.error("Could not initialize PhysicalEntity.\n" + e.getMessage());
            return false;
        }
    }

    public static boolean subscribe() {
        try {
            ambassador.subscribeObjectClassAttributes(objectHandle, attributeHandle);
            return true;
        } catch (AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            Utils.error("Could not subscribe to PhysicalEntity.\n" + e.getMessage());
            return false;
        }
    }

    @Override
    public void setAttributes(AttributeHandleValueMap theAttributes) {
        try {
            setParentName(theAttributes.get(attributePEParentHandle));
        } catch (NullPointerException e) {
        }
        
        try {
            setState(theAttributes.get(attributePEStateHandle));
        } catch (NullPointerException e) {
        }

        try {
            setMultiState(theAttributes.get(attributePEMultiStateHandle));
        } catch (NullPointerException e) {
        }
    }

    private void setMultiState(byte[] bytes) {		
        try {
            SpaceTimeCoordinateStateCoder space = new SpaceTimeCoordinateStateCoder();
            SpaceTimeCoordinateState state = space.decode(bytes);
            
            if(parentName != null) {
            	position = state.getTranslationalState().getPosition().toArray();
 
 	            rotation[0] = state.getRotationState().getAttitudeQuaternion().getQ1();
	            rotation[1] = state.getRotationState().getAttitudeQuaternion().getQ2();
	            rotation[2] = state.getRotationState().getAttitudeQuaternion().getQ3();
	            rotation[3] = state.getRotationState().getAttitudeQuaternion().getScalarPart();
            }
            
            time = state.getTime().getValue().doubleValue();

        } catch (RTIinternalError | DecoderException ex) {
            Logger.getLogger(ReferenceFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
