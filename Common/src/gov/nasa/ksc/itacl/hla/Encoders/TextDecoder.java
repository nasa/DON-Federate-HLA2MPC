package gov.nasa.ksc.itacl.hla.Encoders;


import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;

import hla.rti1516e.encoding.HLAunicodeString;

public class TextDecoder {

	private final HLAunicodeString coder;
	
	public TextDecoder(EncoderFactory factory) {		
		coder = factory.createHLAunicodeString();
		
	}
	
	public String decode( byte [] bytes ) throws DecoderException {
		coder.decode(bytes);
		return coder.getValue();
	}
}
