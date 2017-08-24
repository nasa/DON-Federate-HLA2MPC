package gov.nasa.ksc.itacl.hla.Encoders;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfixedArray;
import hla.rti1516e.encoding.HLAfloat64LE;

public class PositionDecoder {
	private final HLAfixedArray<HLAfloat64LE> coder;
	
	public PositionDecoder(EncoderFactory factory) {
		HLAfloat64LE x = factory.createHLAfloat64LE();
		HLAfloat64LE y = factory.createHLAfloat64LE();
		HLAfloat64LE z = factory.createHLAfloat64LE();
		
		coder = factory.createHLAfixedArray(x,y,z);	
	}

	public double [] decode( byte [] bytes ) throws DecoderException {
		coder.decode(bytes);
		return new double [] { 
				coder.get(0).getValue(),
				coder.get(1).getValue(),
				coder.get(2).getValue()
		};
	}
}
