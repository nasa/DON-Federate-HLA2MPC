package gov.nasa.ksc.itacl.hla.Encoders;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfloat64LE;

public class TimeDecoder {

	private final HLAfloat64LE coder;
	
	public TimeDecoder(EncoderFactory factory) {		
		coder = factory.createHLAfloat64LE();	
	}
	
	public double decode( byte [] bytes ) throws DecoderException {
		coder.decode(bytes);
		return coder.getValue();
	}
}
