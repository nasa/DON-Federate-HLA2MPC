package gov.nasa.ksc.itacl.HLAtoDON.Models;

import hla.rti1516e.AttributeHandleValueMap;

public interface UpdateHLA {
	void setName(String name);
	void setParentName(byte[] bytes);
	void setTime(byte[] bytes);
	void setPosition(byte[] bytes);
	void setRotation(byte[] bytes);
	void setScale(byte[] bytes);
	void setAttributes(AttributeHandleValueMap theAttributes);
}
