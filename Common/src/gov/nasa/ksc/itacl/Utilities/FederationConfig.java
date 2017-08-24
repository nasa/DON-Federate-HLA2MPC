package gov.nasa.ksc.itacl.Utilities;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.FactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class FederationConfig {
	public String federationName;
	public String federateName;
	public String hostAndPortSettings;
	public String description = "";
	public String RTI = "";
	public URL[] fomModules;
	public ConvertObject [] convertedObjects;
	public int federateMPCSocketServerPortNum;
	private static int DEFAULT_FEDERATE_PORT_NUM = 20170;
	private static final String PITCH_LOCAL = "Pitch Local";
		
	public static class ConvertObject {
		private String name;
		private boolean convertPosition = false;
		private boolean convertRotation = false;
		private boolean groundClamp = false;
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setConvertPosition(boolean flag) {
			convertPosition = flag;
		}
		
		public void setConvertRotation(boolean flag) {
			convertRotation = flag;
		}
		
		public void setGroundClamp(boolean flag) {
			groundClamp = flag;
		}		
		
		public String getName(){
			return name;
		}
		
		public boolean getConvertPosition() {
			return convertPosition;
		}
		
		public boolean getConvertRotation() {
			return convertRotation;
		}
		
		public boolean getGroundClamp() {
			return groundClamp;
		} 		
	}
	
	public String toString() {
		String border = "\n****************************************************";
		String str = new String(border);
		str += "\nFederate Name:\n\t" + federateName + "\nFederation Name:\n\t" + federationName +"\nHLA Connection:\n\t" + hostAndPortSettings +"\nFOMs:";
		for(int i = 0; i < fomModules.length; ++i) {
			str += "\n\t" + fomModules[i].toString();
		}
		str+="\nConverted Objects:";
		for(int i = 0; i < convertedObjects.length; ++i){
			str+="\n\t" + convertedObjects[i].getName();
		}
		
		str += border;
		
		return str;
	}
	
	public static FederationConfig createConfig(String pathToConfigFile) {
		
		try {
		
			FederationConfig config = new FederationConfig();
			
			Document doc = createXmlDocument(pathToConfigFile);
			Utils.info("Config file: " + pathToConfigFile);			
			if( doc != null ) {
				config.federateName = getFederateName(doc);
				config.federateMPCSocketServerPortNum = getFederateMPCSocketServerPortNum(doc);
				Node federationNode = getFederationInfo(doc, getFederationId(doc));
				config.federationName = getAttributeValue(federationNode, "name");
				String host =  getAttributeValue(federationNode, "host");
				String port = getAttributeValue(federationNode, "port");
				config.hostAndPortSettings = new String("Host=" + host + "\n\tPort=" + port );
				config.fomModules = getFomMudules(doc);
				config.convertedObjects = getConvertedObjects(doc);
				config.description = getAttributeValue(federationNode, "description");
				config.RTI = getAttributeValue(federationNode, "rti");
			}
			return config;
			
		}
		catch(Exception e) {
			Utils.error("Possible malformed config file. Could not load " + pathToConfigFile + "\n" + e.getMessage() );
			return null;
		}
	
	}
	
	private static ConvertObject [] getConvertedObjects(Document doc) {
		NodeList objectsNodes = doc.getElementsByTagName("object");
		ConvertObject [] objects = new ConvertObject [objectsNodes.getLength()];
		for(int i =0; i < objectsNodes.getLength(); ++i){
			Node node = objectsNodes.item(i);
			ConvertObject cObject = new ConvertObject();
			cObject.setName(getAttributeValue(node, "name"));
			cObject.setConvertPosition(Boolean.parseBoolean(getAttributeValue(node, "convertPos")));
			cObject.setConvertRotation(Boolean.parseBoolean(getAttributeValue(node, "convertRot")));
//			cObject.setGroundClamp(Boolean.parseBoolean(getAttributeValue(node, "groundClamp")));			
			objects[i] = cObject;
		}
		return objects;
	}

	private static URL[] getFomMudules(Document doc) {
		NodeList fomNodes = doc.getElementsByTagName("fom");
		URL [] urls = new URL [fomNodes.getLength()];
		for(int i = 0; i < fomNodes.getLength(); ++i){
			try {
				Node node = fomNodes.item(i);
				File file = new File(getAttributeValue(node, "uri"));
				urls[i] = file.toURI().toURL();
			}
			catch (MalformedURLException e) {
				Utils.info("Could not get a fom url.\n" + e.getMessage());
			}
		}
		return urls;
	}

	private static Node getFederationInfo(Document doc, String configName) {
		NodeList nodeList = doc.getElementsByTagName("federation");
		for(int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			String name = node.getAttributes().getNamedItem("id").getNodeValue();
			if( name.equals(configName)) {
				return node;
			}
		}
		return null;
		
	}	
	
	private static String getFederateName(Document doc) {
		Node node = doc.getElementsByTagName("federate").item(0);
		if(node != null ) {
			return getAttributeValue(node,"name");
		}
		return null;
	}
	
	private static int getFederateMPCSocketServerPortNum(Document doc) {
		Node node = doc.getElementsByTagName("federate").item(0);
		if(node != null ) {
			try {
				int portNum = Integer.valueOf(getAttributeValue(node,"mpcPort"));
				return portNum;
			}
			catch (NumberFormatException e) {
				Utils.error("Could not get MPC SocketStream port. Setting it to default port.\n" + e.getMessage());
				return DEFAULT_FEDERATE_PORT_NUM;
			}
		}
		return DEFAULT_FEDERATE_PORT_NUM;
	}	
	
	private static Document createXmlDocument(String pathToConfigFile) {
		try {
			File file = new File(pathToConfigFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbFactory.newDocumentBuilder();
			Document doc = builder.parse(file);
			return doc;
		} 
		catch (ParserConfigurationException | FactoryConfigurationError | SAXException | IOException e) {
			Utils.error("Could not read xml config file.\n" + e.getMessage());
			return null;
		}
	}
	
	private static String getFederationId(Document doc) {
		Node node = doc.getElementsByTagName("federate").item(0);
		if(node != null ) {
			return getAttributeValue(node,"federationId");
		}
		return PITCH_LOCAL;
	}
	private static String getAttributeValue(Node node, String attributeName) {
		return node.getAttributes().getNamedItem(attributeName).getNodeValue();
	}
	
	
} 
