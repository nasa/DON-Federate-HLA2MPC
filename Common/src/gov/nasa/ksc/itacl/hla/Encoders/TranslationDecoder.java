package gov.nasa.ksc.itacl.hla.Encoders;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfixedArray;
import hla.rti1516e.encoding.HLAfixedRecord;
import hla.rti1516e.encoding.HLAfloat64LE;

public class TranslationDecoder {
	private final HLAfixedRecord coder;
	private final HLAfixedArray<HLAfloat64LE> positionCoder;
	private final HLAfixedArray<HLAfloat64LE> velocityCoder;
	
	public TranslationDecoder(EncoderFactory factory) {
		HLAfloat64LE x = factory.createHLAfloat64LE();
		HLAfloat64LE y = factory.createHLAfloat64LE();
		HLAfloat64LE z = factory.createHLAfloat64LE();
		
		
		HLAfloat64LE vx = factory.createHLAfloat64LE();
		HLAfloat64LE vy = factory.createHLAfloat64LE();
		HLAfloat64LE vz = factory.createHLAfloat64LE();
		
		coder = factory.createHLAfixedRecord();
		positionCoder = factory.createHLAfixedArray(x,y,z);
		velocityCoder = factory.createHLAfixedArray(vx,vy,vz);
		coder.add(positionCoder );
		coder.add(velocityCoder);
		
	}
	
	public double [][] decode( byte [] bytes ) throws DecoderException {
		coder.decode(bytes);
		return new double [] [] { 
				new double[] { 
						positionCoder.get(0).getValue(),
						positionCoder.get(1).getValue(),
						positionCoder.get(2).getValue()
				},
				new double[] {
						velocityCoder.get(0).getValue(),
						velocityCoder.get(1).getValue(),
						velocityCoder.get(2).getValue(),
				}
		};
	}
}
