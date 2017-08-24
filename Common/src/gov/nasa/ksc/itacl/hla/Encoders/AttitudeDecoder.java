package gov.nasa.ksc.itacl.hla.Encoders;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfixedArray;
import hla.rti1516e.encoding.HLAfixedRecord;
import hla.rti1516e.encoding.HLAfloat64LE;

public class AttitudeDecoder {

	private final HLAfixedRecord coder;
	private final HLAfixedRecord rotationCoder;
	private final HLAfixedArray<HLAfloat64LE> vectorCoder;
	private final HLAfloat64LE scalerCoder;
	
	public AttitudeDecoder(EncoderFactory factory) {
		
		HLAfloat64LE x = factory.createHLAfloat64LE();
		HLAfloat64LE y = factory.createHLAfloat64LE();
		HLAfloat64LE z = factory.createHLAfloat64LE();
		
		coder = factory.createHLAfixedRecord();
		rotationCoder = factory.createHLAfixedRecord();
		vectorCoder = factory.createHLAfixedArray(x,y,z);	
		scalerCoder = factory.createHLAfloat64LE();
	
		rotationCoder.add(scalerCoder);
		rotationCoder.add(vectorCoder);
	
		coder.add( rotationCoder );
	}
	
	public double[] decode(byte[] bytes) throws DecoderException {
		coder.decode(bytes);
		return new double [] { 
				vectorCoder.get(0).getValue(),
				vectorCoder.get(1).getValue(),
				vectorCoder.get(2).getValue(),
				scalerCoder.getValue()
		};
	}
}
