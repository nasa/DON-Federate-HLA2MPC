package gov.nasa.ksc.itacl.hla.Encoders;

import gov.nasa.ksc.itacl.Utilities.Utils;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfixedRecord;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpaceTimeCoordinateStateDecoder {

    private final HLAfixedRecord coder;

    public SpaceTimeCoordinateStateDecoder(EncoderFactory factory) {
        coder = factory.createHLAfixedRecord();
    }
    
    public void decode(byte [] bytes) {
        try {
        	ByteWrapper byteWrapper = new ByteWrapper(bytes);
            coder.decode(byteWrapper);
            if(coder.iterator() != null) {
            Utils.log("Iterator is: " + coder.iterator().toString());
            }
            else {
            	Utils.log("Iterator is null");
            }
        } catch (DecoderException ex) {
            Utils.log(ex.toString());
            Logger.getLogger(SpaceTimeCoordinateStateDecoder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int size() {
        return coder.size();
    }

    public DataElement get(int index) {
        return coder.get(index);
    }
}
