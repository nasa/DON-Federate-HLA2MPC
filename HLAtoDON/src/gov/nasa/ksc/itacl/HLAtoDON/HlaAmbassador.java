package gov.nasa.ksc.itacl.HLAtoDON;

import java.util.HashMap;
import java.util.Map;
import hla.rti1516e.*;
import hla.rti1516e.time.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.encoding.*;
import gov.nasa.ksc.itacl.hla.Encoders.AttitudeDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.PositionDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.RotationalDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TextDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TimeDecoder;
import gov.nasa.ksc.itacl.hla.Encoders.TranslationDecoder;
import gov.nasa.ksc.itacl.HLAtoDON.Models.PhysicalEntity;
import gov.nasa.ksc.itacl.HLAtoDON.Models.ReferenceFrame;
import gov.nasa.ksc.itacl.HLAtoDON.Models.UpdateHLA;
import gov.nasa.ksc.itacl.mpc.models.MPCObject;
import gov.nasa.ksc.itacl.mpc.models.Telemetry;
import gov.nasa.ksc.itacl.Utilities.*;
import gov.nasa.ksc.itacl.Utilities.FederationConfig.ConvertObject;


public class HlaAmbassador extends NullFederateAmbassador implements HlaInterface {

    private static final String FEDERATE_TYPE = "HLA to DON";

    private static final String HLA = "HLA";

    private static final String DESCRIPTION = "Description";

    private static final String P_RTI_1516 = "pRTI 1516";

    // Look ahead time of 1 second in Microseconds
    private static final long LOOKAHEAH_MICRO_SEC = 1000000;

    // Microsecond to Second.
    private static final double MICRO_SEC_TO_SEC = 0.0000010;

    private RTIambassador ambassador;
    private FederationConfig config;

    // Telemetry Object
    private Telemetry telemetry;

    // Encoder and Decoders
    private TranslationDecoder translationDecoder;
    private TextDecoder textDecoder;
    private RotationalDecoder rotationalDecoder;
    private AttitudeDecoder attitudeDecoder;
    private PositionDecoder positionDecoder;
    private TimeDecoder timeDecoder;

    // Mapping object instance to object class map
    private Map<ObjectInstanceHandle, ObjectClassHandle> registeredObjects = new HashMap<ObjectInstanceHandle, ObjectClassHandle>();

    // HLA RTI connected status
    private boolean isRTIconnected = false;

    // HLA RTI shutdown status
    private boolean isRTIshutdown = false;

    // HLA RTI time constrained status
    private boolean isRTItimeConstrained = false;

    // HLA RTI advancing time status
    private boolean isRTIadvancingTime;

    // HLA RTI first update status
    private boolean isRTIfirstUpdate = false;

    // HLA RTI new attribute update for time frame
    private boolean isRTInewUpdate = false;

    // HLA Time management 
    private HLAinteger64TimeFactory timeFactory;
    private HLAinteger64Time currentTime;
    private HLAinteger64Interval lookAhead;

    public HlaAmbassador() {
    }

    private String getCorrectConnectionString(String rtiName) {
        String connectionString = new String();
        if (rtiName.equals(P_RTI_1516)) {
            connectionString = this.config.hostAndPortSettings;
            Utils.info("connection string is " + connectionString);
        } else if (rtiName.equals("MAK RTI")) {
            /*
	    	 * TODO for MAK. for now just return null.
	    	 * 
             */
            connectionString = null;
        }

        return connectionString;
    }

    private void initEncoders(EncoderFactory encoderFactory) {

        //Reference Frame
        this.rotationalDecoder = new RotationalDecoder(encoderFactory);
        this.translationDecoder = new TranslationDecoder(encoderFactory);
        this.textDecoder = new TextDecoder(encoderFactory);
        this.timeDecoder = new TimeDecoder(encoderFactory);

        ReferenceFrame.translationDecoder = this.translationDecoder;
        ReferenceFrame.rotationalDecoder = this.rotationalDecoder;
        ReferenceFrame.textDecoder = this.textDecoder;
        ReferenceFrame.timeDecoder = this.timeDecoder;

        this.attitudeDecoder = new AttitudeDecoder(encoderFactory);
        this.positionDecoder = new PositionDecoder(encoderFactory);

        PhysicalEntity.attitudeDecoder = this.attitudeDecoder;
        PhysicalEntity.positionDecoder = this.positionDecoder;
        PhysicalEntity.textDecoder = this.textDecoder;
        PhysicalEntity.timeDecoder = this.timeDecoder;
    }

    private double getDoubleTime(HLAinteger64Time time) {
        return time.getValue() * MICRO_SEC_TO_SEC;
    }

    private void addObjectToTelemetry(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) {
        this.registeredObjects.put(theObject, theObjectClass);
        try {
            if (objectName != null) {
                this.telemetry.registerObject(objectName);
                Utils.info("Register object: " + objectName);
            }

        } catch (Exception e) {
            Utils.warn("Could not register object.\n" + objectName);
        }
    }

    private void updateAttributes(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes) {

        UpdateHLA update = null;
        String name;

        ObjectClassHandle classHandle = this.registeredObjects.get(theObject);
        double time = getDoubleTime();

        //get the sim object
        try {
            name = this.ambassador.getObjectInstanceName(theObject);
        } catch (Exception e1) {
            Utils.error("Could not get SimulationObject for " + theObject.toString() + ". Skipping...");
            return;
        }

        //create the update
        try {
            if (classHandle.equals(ReferenceFrame.objectHandle)) {
                update = new ReferenceFrame();

            } else if (classHandle.equals(PhysicalEntity.objectHandle)) {
                update = new PhysicalEntity();
            }
		
            update.setName(name);
            update.setAttributes(theAttributes);

        } catch (Exception e) {
            Utils.warn("Could not update attributes for " + name + ".\n" + e.getMessage());
        }

        if (update != null) {

            MPCObject mpcObject = (MPCObject) update;
            try {

                ConvertObject[] cObjects = this.config.convertedObjects;

                for (int i = 0; i < cObjects.length; ++i) {
                    ConvertObject cObject = cObjects[i];
                    if (cObject.getName().equals(mpcObject.getName())) {
                        mpcObject.setHeightAboveTerrain(0);
                        mpcObject.setConvertPosition(cObject.getConvertPosition());
                        mpcObject.setConvertRotation(cObject.getConvertRotation());
                        mpcObject.setGroundClamp(cObject.getGroundClamp());                        
                    }
                }
            } catch (Exception e) {
                Utils.warn("Could not add convert for " + name + "\n" + e.getMessage());
            }

            try {
                // Add SolarSystemBarycentricInertial Reference Frame if it's first update
                if (this.isRTInewUpdate) {
                    this.isRTInewUpdate = false;
                    try {

                        ReferenceFrame solarSystem = new ReferenceFrame();
                        solarSystem.setName("SolarSystemBarycentricInertial");
                        solarSystem.setRotation(new double[]{0, 0, 0, 1});
                        this.telemetry.add(getDoubleTime(), solarSystem);

                    } catch (Exception e) {
                        Utils.error("Cannot add " + name + " to current update\n" + e.getMessage());
                    }
                }

                // Add mission level metadata if it's first update
                if (this.isRTIfirstUpdate) {
                    this.isRTIfirstUpdate = false;
                    System.out.println(mpcObject.getPrettyTime());
                    this.telemetry.addMissionMetadata("Epoch", mpcObject.getPrettyTime());
                }
                this.telemetry.add(time, mpcObject);
            } catch (Exception e) {
                Utils.warn("Could not add update for " + name + " @ " + time + "\n" + e.getMessage());
            }
        }
    }

    // start of HlaInterface Overrides
    @Override
    public boolean start(FederationConfig config, Telemetry telemetry) {
        this.config = config;
        this.telemetry = telemetry;

        RtiFactory rtiFactory = null;
        try {
            rtiFactory = RtiFactoryFactory.getRtiFactory();
            this.ambassador = rtiFactory.getRtiAmbassador();
        } catch (RTIinternalError e) {
            Utils.error("Could not create RTI ambassador or factory.\n" + e.getMessage());
            return false;
        }

        // Connect to the federation
        try {
            if (rtiFactory.rtiName() == null) {
                Utils.error("rti name is null");
                return false;
            }
            String connectionString = getCorrectConnectionString(rtiFactory.rtiName());
            if (connectionString == null) {
                return false;
            }
            this.ambassador.connect(this, CallbackModel.HLA_IMMEDIATE, connectionString);
        } catch (AlreadyConnected ignore) {
        } catch (CallNotAllowedFromWithinCallback | UnsupportedCallbackModel | ConnectionFailed | InvalidLocalSettingsDesignator | RTIinternalError e) {
            Utils.error("Could not connect to the RTI.\n " + e.getMessage());
            return false;
        }

        // Destroy the federation
        try {
            this.ambassador.destroyFederationExecution(config.federationName);
        } catch (FederatesCurrentlyJoined | FederationExecutionDoesNotExist ignore) {
        } catch (NotConnected | RTIinternalError e) {
            Utils.error("Could not destroy federation Execution");
            return false;
        }

        // Create the federation
        try {
            this.ambassador.createFederationExecution(config.federationName, config.fomModules, "HLAinteger64Time");
        } catch (FederationExecutionAlreadyExists ignored) {
        } catch (InconsistentFDD | ErrorReadingFDD | CouldNotOpenFDD | NotConnected | RTIinternalError e) {
            Utils.error("Could not create federation Execution. " + e.getMessage());
            return false;
        } catch (CouldNotCreateLogicalTimeFactory e) {
            Utils.error("Could not create logical time factory. " + e.getMessage());
            return false;
        }

        // create a unique name and join the federation
        try {
            boolean joined = false;
            int federateNameIndex = 0;
            while (!joined) {
                try {

                    String name = config.federateName;
                    if (federateNameIndex > 0) {
                        name += " " + federateNameIndex;
                    }

                    Utils.info("Trying federate name:" + name);
                    this.ambassador.joinFederationExecution(name, FEDERATE_TYPE, config.federationName, config.fomModules);
                    joined = true;

                } catch (FederateNameAlreadyInUse fe) {
                    federateNameIndex++;
                } catch (InconsistentFDD | ErrorReadingFDD | CouldNotOpenFDD e) {
                    Utils.error("Issues with the FOM files: " + e.getMessage());
                    return false;
                }

            }
        } catch (SaveInProgress | RestoreInProgress | NotConnected | CouldNotCreateLogicalTimeFactory
                | FederationExecutionDoesNotExist | FederateAlreadyExecutionMember | RTIinternalError e) {
            Utils.error("Could not join federation execution.\n" + e.getMessage());
            return false;
        } catch (CallNotAllowedFromWithinCallback e2) {
            Utils.error("Could not join federation execution.\n" + e2.getMessage());
            return false;
        }

        try {
            EncoderFactory encoderFactory = rtiFactory.getEncoderFactory();
            initEncoders(encoderFactory);
        } catch (RTIinternalError e) {
            Utils.error("Could not create encoders");
            return false;
        }
        this.isRTIconnected = true;

        this.isRTIfirstUpdate = true;

        this.telemetry.addMissionMetadata(DESCRIPTION, config.description);
        this.telemetry.addMissionMetadata(HLA, config.RTI);

        return true;
    }

    @Override
    public boolean disconnect() {
        //Finally disconnect
        try {
            this.ambassador.disconnect();
        } catch (FederateIsExecutionMember | CallNotAllowedFromWithinCallback | RTIinternalError e) {
            Utils.error("Could not disconnect from RTI.\n" + e.getMessage());
            return false;
        }

        this.isRTIconnected = false;
        this.isRTIshutdown = true;
        return true;
    }

    @Override
    public boolean isConnected() {
        return this.isRTIconnected;
    }

    @Override
    public void advanceTime() {
        this.isRTIadvancingTime = true;
        try {
            @SuppressWarnings("rawtypes")
            LogicalTime nextTime = this.currentTime.add(this.lookAhead);
            //double t = getDoubleTime((HLAinteger64Time)nextTime);
            this.ambassador.timeAdvanceRequest(nextTime);
            while (this.isRTIadvancingTime) {
                Thread.sleep(Utils.THREAD_SLEEP_TIME);
            }
        } catch (LogicalTimeAlreadyPassed | InvalidLogicalTime
                | InTimeAdvancingState | RequestForTimeRegulationPending
                | RequestForTimeConstrainedPending | SaveInProgress
                | RestoreInProgress | FederateNotExecutionMember | NotConnected
                | RTIinternalError | IllegalTimeArithmetic e) {
            Utils.error("Could not request to advance time. " + e.getMessage());
        } catch (InterruptedException e) {
            Utils.error("Could not advance time.\n" + e.getMessage());
        }
    }

    @Override
    public boolean isShutdown() {
        return this.isRTIshutdown;
    }

    @Override
    public double getDoubleTime() {
        return getDoubleTime(this.currentTime);
    }

    @Override
    public double getPreviousTime() {
        try {
            return getDoubleTime(this.currentTime.subtract(this.lookAhead));
        } catch (IllegalTimeArithmetic e) {
            Utils.warn("Could not get previous time. Returning current time");
            return getDoubleTime();
        }
    }

    @Override
    public boolean advanceToGALT() {
        try {
            TimeQueryReturn starting_GALT = this.ambassador.queryGALT();
            HLAinteger64Time galt = (HLAinteger64Time) starting_GALT.time;
            this.currentTime = galt;
            Utils.info("current time: " + getDoubleTime(galt));

            this.ambassador.timeAdvanceRequest(galt);
            this.isRTIadvancingTime = true;
            while (this.isRTIadvancingTime) {
                Thread.sleep(Utils.THREAD_SLEEP_TIME);
            }
            return true;
        } catch (LogicalTimeAlreadyPassed | InvalidLogicalTime e) {
            Utils.error("Could not advance to GALT. " + e.getMessage());
            return false;
        } catch (InTimeAdvancingState | RequestForTimeRegulationPending | RequestForTimeConstrainedPending e) {
            Utils.error("Could not advance to GALT. " + e.getMessage());
            return false;
        } catch (SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            Utils.error("Could not advance to GALT. " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Utils.error("Could not advance to GALT. " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean subscribe() {

        return (ReferenceFrame.subscribe() && PhysicalEntity.subscribe());
    }

    @Override
    public boolean publish() {
        return false;
    }

    @Override
    public boolean initHandles() {
        return (ReferenceFrame.Initialize(this.ambassador) && PhysicalEntity.Initialize(this.ambassador));
    }

    @Override
    public boolean enableTimeConstrained() {
        // Get the logical time factory.
        try {

            this.timeFactory = (HLAinteger64TimeFactory) this.ambassador.getTimeFactory();

            // Make the local logical time interval.
            this.lookAhead = this.timeFactory.makeInterval(LOOKAHEAH_MICRO_SEC);

            // Enable time constraint.
            this.ambassador.enableTimeConstrained();

            // Wait for time constrained to take affect.
            while (!this.isRTItimeConstrained) {
                Thread.sleep(Utils.THREAD_SLEEP_TIME);
            }

            return true;

        } catch (FederateNotExecutionMember | NotConnected | SaveInProgress | RestoreInProgress | RTIinternalError e) {
            Utils.error("Could not enable time constraint. " + e.getMessage());
            return false;

        } catch (InTimeAdvancingState | RequestForTimeConstrainedPending | TimeConstrainedAlreadyEnabled e1) {
            Utils.error("Could not enable time constraint. " + e1.getMessage());
            return false;

        } catch (InterruptedException e2) {
            Utils.error("Could not enable time constraint. " + e2.getMessage());
            return false;
        }
    }

    @Override
    public boolean disableTimeConstrained() {
        try {
            this.ambassador.disableTimeConstrained();
            return true;
        } catch (TimeConstrainedIsNotEnabled e) {
            Utils.warn("Could not disable time constrained. \n" + e.getMessage());
            return true;
        } catch (SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            Utils.warn("Could not disable time constrained. \n" + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean enableAsynchonousDelivery() {
        try {
            this.ambassador.enableAsynchronousDelivery();
        } catch (AsynchronousDeliveryAlreadyEnabled | SaveInProgress | RestoreInProgress ignore) {
        } catch (FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            Utils.error("Could not enable asynchonous delivery.\n " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean resignFederationExecution() {
        try {
            this.ambassador.resignFederationExecution(ResignAction.CANCEL_THEN_DELETE_THEN_DIVEST);
        } catch (InvalidResignAction | OwnershipAcquisitionPending | FederateOwnsAttributes | FederateNotExecutionMember | NotConnected | CallNotAllowedFromWithinCallback | RTIinternalError e) {
            Utils.warn("Could not resign federation execution.\n" + e.getMessage());
            return false;
        }

        if (this.config.federationName != null) {
            try {
                this.ambassador.destroyFederationExecution(this.config.federationName);
            } catch (FederatesCurrentlyJoined | FederationExecutionDoesNotExist | NotConnected ignore) {
            } catch (RTIinternalError e) {
                Utils.warn("Could not destroy federation Execution." + e.getMessage());
                return false;
            }
        }
        return true;
    }
    // end of HlaInterface Overrides

    //Start of NullFederateAmbassador Overrides	
    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) {
        addObjectToTelemetry(theObject, theObjectClass, objectName);
    }

    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName, FederateHandle producingFederate) {
        addObjectToTelemetry(theObject, theObjectClass, objectName);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
            AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag,
            OrderType sentOrdering,
            TransportationTypeHandle theTransport,
            @SuppressWarnings("rawtypes") LogicalTime theTime,
            OrderType receivedOrdering,
            FederateAmbassador.SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {

        updateAttributes(theObject, theAttributes);

    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
            AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag,
            OrderType sentOrdering,
            TransportationTypeHandle theTransport,
            @SuppressWarnings("rawtypes") LogicalTime theTime,
            OrderType receivedOrdering,
            MessageRetractionHandle retractionHandle,
            FederateAmbassador.SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {

        updateAttributes(theObject, theAttributes);

    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
            AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag,
            OrderType sentOrdering,
            TransportationTypeHandle theTransport,
            FederateAmbassador.SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {

        updateAttributes(theObject, theAttributes);

    }

    @Override
    public void timeAdvanceGrant(@SuppressWarnings("rawtypes") LogicalTime theTime) throws FederateInternalError {
        this.currentTime = (HLAinteger64Time) theTime;
        this.isRTIadvancingTime = false;
        this.isRTInewUpdate = true;

    }

    @Override
    public void timeConstrainedEnabled(@SuppressWarnings("rawtypes") LogicalTime time) throws FederateInternalError {
        this.isRTItimeConstrained = true;
    }
    //End of NullFederateAmbassador Overrides	

}
