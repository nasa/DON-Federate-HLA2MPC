package gov.nasa.ksc.itacl.mpc;

import gov.nasa.ksc.itacl.Utilities.Utils;
import gov.nasa.ksc.itacl.mpc.models.MPCObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

public class MPCWriter {
	private XMLEventWriter eventWriter;
	private XMLEventFactory eventFactory;
	private OutputStream stream;

	public MPCWriter( OutputStream stream ){
		try {
			this.stream = stream;
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			eventFactory = XMLEventFactory.newInstance();
			eventWriter = outputFactory.createXMLEventWriter(stream);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException {
		stream.close();	
	}

	private StartElement createStartElement(String tagName) throws XMLStreamException {
		StartElement startElement = eventFactory.createStartElement("","", tagName);
		eventWriter.add(startElement);
		return startElement;
	}
	
	private void createEndElement(String tagName) throws XMLStreamException {
		EndElement endElement = eventFactory.createEndElement("", "", tagName);
	    eventWriter.add(endElement);	
	}
	
	public void createStartDocument() throws XMLStreamException {
		StartDocument startDocument = eventFactory.createStartDocument();
		eventWriter.add(startDocument);
		createStartElement("mpc");
		eventWriter.flush();
	}
	
	public void createEndDocument() throws XMLStreamException {
		createEndElement("mpc");
		EndDocument endDocument = eventFactory.createEndDocument();
		eventWriter.add(endDocument);
		eventWriter.close();
	}	

	public void createStartTime(String time) throws XMLStreamException {
		createStartElement("time");
		Attribute attr = eventFactory.createAttribute("value", time);
		eventWriter.add(attr);
		eventWriter.flush();		
	}
	
	public void createEndTime() throws XMLStreamException {
		createEndElement("time");
		eventWriter.flush();
	}	

	public void createStartObject(String name) throws XMLStreamException {
		createStartElement("object");
		Attribute attr = eventFactory.createAttribute("id", name);
		eventWriter.add(attr);
		eventWriter.flush();
	}
	
	public void createEndObject() throws XMLStreamException {
		createEndElement("object");
		eventWriter.flush();
	}
	
	public void createStartPosition(double[] position, boolean convertPosition) throws XMLStreamException {
		createStartElement("pos");
	
		Attribute attr = null;
		if(convertPosition) {
			attr = eventFactory.createAttribute("convert", String.valueOf(convertPosition));
			eventWriter.add(attr);
		}
		
		String sPosition = Double.toString(position[0])+" "+Double.toString(position[1])+" "+ Double.toString(position[2]);
		Characters characters = eventFactory.createCharacters(sPosition);
	    eventWriter.add(characters);
	    eventWriter.flush();
	}

	public void createEndPosition() throws XMLStreamException {
		createEndElement("pos");
		eventWriter.flush();
	}
	
	public void createStartRotation(double[] rotation, boolean convertRotation) throws XMLStreamException {
		createStartElement("quat");

		if(convertRotation) {
			Attribute attr = eventFactory.createAttribute("convert", String.valueOf(convertRotation));
			eventWriter.add(attr);
		}
		
		String sRotation = Double.toString(rotation[0])+" "+Double.toString(rotation[1])+" "+ Double.toString(rotation[2]) + " " +Double.toString(rotation[3]) ;
		Characters characters = eventFactory.createCharacters(sRotation);
	    eventWriter.add(characters);   
	    eventWriter.flush();
	}
	
	public void createEndRotation() throws XMLStreamException {
		createEndElement("quat");
		eventWriter.flush();
	}
	
	public void createStartParent() throws XMLStreamException {
		createStartElement("parent");	
		eventWriter.flush();
	}
	
	public void createStartParent(String parentName) throws XMLStreamException {
		createStartElement("parent");	
		Characters characters = eventFactory.createCharacters(parentName);
		eventWriter.add(characters);
		eventWriter.flush();
	}
	
	public void createEndParent() throws XMLStreamException {
		createEndElement("parent");
		eventWriter.flush();
	}
	
	public void createMetaData(String key, String value) throws XMLStreamException {
		createStartElement("metaData");
		Attribute attr = eventFactory.createAttribute("id", key);
		Characters characters = eventFactory.createCharacters(value);
		eventWriter.add(attr);
		eventWriter.add(characters);
		createEndElement("metaData");
		eventWriter.flush();
	}
	
	public void createVisible(boolean visible) throws XMLStreamException {
		createStartElement("vis");
		String stringVis = String.valueOf(visible ? 1:0);
		Characters characters = eventFactory.createCharacters(stringVis);
		eventWriter.add(characters);
		createEndElement("vis");
		eventWriter.flush();
	}
	
	public void createTime(Double second, Collection<MPCObject> updates) throws XMLStreamException {
		createStartTime(String.valueOf(second));
		Iterator<MPCObject> ir = updates.iterator();
		while(ir.hasNext()){
			MPCObject obj = ir.next();
			createStartObject( obj.getName());
			Map<String,String> meta = obj.getMetaData();
			if(meta != null) {
				Iterator<Entry<String,String>> it = meta.entrySet().iterator();
				while(it.hasNext()) {
					Entry<String, String> entry = it.next();
					createMetaData(entry.getKey(), entry.getValue());
				}
			}
			createStartPosition( obj.getPosition(), obj.getConvertPosition());
			createEndPosition();
			createStartRotation( obj.getRotation(), obj.getConvertRotation());
			createEndRotation();	
			if(checkforEmptyParentName(obj.getParentName()) == false) {
				createStartParent(obj.getParentName());
			}
			else {
				createStartParent();				
			}
			createEndParent();
			createVisible(obj.isVisible());
			createEndObject();
		}
		createEndTime();
	}

	private Boolean checkforEmptyParentName(String parentName) {
		if(parentName == null) {
			return true;
		}
		try {
			for(int i=0; i<parentName.length(); ++i) {
				char ch = parentName.charAt(i);
				if(!Character.isDigit(ch) && !Character.isLetter(ch) && !Character.isSpaceChar(ch)) {
					return true;
				}
			}
			return false;	
		} catch (IndexOutOfBoundsException e) { 
			Utils.error(" Could not check for empty parent name.\n" +e.getMessage());
			return true;
		}

	}	
}
