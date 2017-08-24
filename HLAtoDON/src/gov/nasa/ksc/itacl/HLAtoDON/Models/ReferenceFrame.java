package gov.nasa.ksc.itacl.HLAtoDON.Models;

import ReferenceFrame.coder.SpaceTimeCoordinateStateCoder;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.siso.spacefom.frame.SpaceTimeCoordinateState;


public class ReferenceFrame extends MPCObject implements UpdateHLA {

    public static TranslationDecoder translationDecoder;
    public static TextDecoder textDecoder;
    public static RotationalDecoder rotationalDecoder;
    public static TimeDecoder timeDecoder;

    //Reference frame attributes
    private static AttributeHandle attributeRFNameHandle;
    private static AttributeHandle attributeRFParentNameHandle;
    private static AttributeHandle attributePEMultiStateHandle;

    private static RTIambassador ambassador;

    public static ObjectClassHandle objectHandle;
    public static AttributeHandleSet attributeHandle;

    public ReferenceFrame() {
    }

    @Override
    public void setPosition(byte[] bytes) {
        try {
            double[][] translation = translationDecoder.decode(bytes);
            position = translation[0];
            double[] velocity = translation[1];
            setMetaData("Velocity", "" + velocity[0] + " " + velocity[1] + " " + velocity[2]);
        } catch (DecoderException e) {
            Utils.warn("Could not decode position for " + getName() + "\n Using defaults!");
            position = new double[]{0, 0, 0};
        }
    }

    @Override
    public void setRotation(byte[] bytes) {
        try {
            rotation = rotationalDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode rotation for " + getName() + "\n Using defaults!");
            rotation = new double[]{0, 0, 0, 1};
        }
    }

    @Override
    public void setScale(byte[] bytes) {
        try {
            throw new Exception("setScale not yet implemented");
        } catch (Exception e) {
            Utils.warn("Could not decode scale for " + getName() + "\n Using defaults");
            scale = new double[]{1, 1, 1};
        }
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

    @Override
    public void setTime(byte[] bytes) {
        try {
            time = timeDecoder.decode(bytes);
        } catch (DecoderException e) {
            Utils.warn("Could not decode name for " + getName() + "\n Using defaults");
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
        } catch (AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            Utils.error("Could not subscribe to Reference Frame.\n" + e.getMessage());
            return false;
        }
    }

    public static boolean Initialize(RTIambassador Ambassador) {
        ambassador = Ambassador;

        //Reference Frame Object
        try {

            objectHandle = ambassador.getObjectClassHandle("ReferenceFrame");

            //Reference Frame Object Attributes
            attributeRFNameHandle = ambassador.getAttributeHandle(objectHandle, "name");

            attributeRFParentNameHandle = ambassador.getAttributeHandle(objectHandle, "parent_name");

            attributePEMultiStateHandle = ambassador.getAttributeHandle(objectHandle, "state");

            //Attribute set for ReferenceFrame
            attributeHandle = ambassador.getAttributeHandleSetFactory().create();

            attributeHandle.add(attributeRFNameHandle);

            attributeHandle.add(attributeRFParentNameHandle);

            attributeHandle.add(attributePEMultiStateHandle);

            return true;

        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidObjectClassHandle e) {
            Utils.error("Could not initialize Reference frame.\n" + e.getMessage());
            return false;
        }

    }

    @Override
    public void setAttributes(AttributeHandleValueMap theAttributes) {
        try {
            setParentName(theAttributes.get(attributeRFParentNameHandle));
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
