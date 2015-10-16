package com.venu.develop.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import com.venu.develop.model.*;
/**
 * Stax parser.
 * Before parsing the xml validate it.
 *
 */

@Component("orderParser")
public class OrderParser {

	private String xmlString;
	private final String ORDER_SCHEMA_DOC = "src/main/resources/order.xsd"; //location of the xsd file
	/*
	private String xmlString = "<?xml version='1.0'?><order>" + "<from zip='10001' state='NY' city='NEW YORK'/>"
			+ "<to zip='20001' state='DC' city='WASHINGTON'/>" + "<lines>"
			+ "<line weight='1000.1' volume='1' hazard='true' product='petrol'/>"
			+ "<line weight='2000' volume='2' hazard='false' product='water'/>" + "</lines>"
			+ "<instructions>here be dragons</instructions>" + "</order>";
	*/
	public static void main(String[] args) throws IOException, XMLStreamException {
		OrderParser orderParser = new OrderParser();
		MultipartFile file = null; //inject the bean param for the file to test
		orderParser.parse( file );
	}
	
	/*
	 * parse the input xml
	 * before parsing,  validate the xml if it is complying with the specific xsd.
	 * parse the xml only if it is a valid one.
	 */

	public Order parse(MultipartFile file) throws IOException, XMLStreamException {

		boolean bInstructions = false;
		boolean isValidXML = false;
		Order order = null;
		String xmlFileData=null;		

		Address fromAddress = new Address();
		Address toAddress = new Address();
		LineItem lineItem = null;
		List<LineItem> lineItems = null;
		
		xmlFileData = new String(file.getBytes());

		xmlString = xmlFileData;
		
		isValidXML = validateXMLSchema(ORDER_SCHEMA_DOC, xmlString);
		if (!isValidXML )  {
			 System.out.println("we are unable to process the input document. the input file does not comply with the schema");
			 return null;
		}
		

		try {
			InputStream in = IOUtils.toInputStream(xmlString, "UTF-8");
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(in);

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				String instructions="";
				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					StartElement startElement = event.asStartElement();
					String qName = startElement.getName().getLocalPart();
					if (qName.equalsIgnoreCase("order")) {
						order = new Order();
					}
					if (qName.equalsIgnoreCase("lines")) {
						lineItems = new ArrayList<LineItem>();
						//System.out.println("Start Element : lines");
					}
					if (qName.equalsIgnoreCase("from")) {
						//System.out.println("Start Element : from");
						fromAddress = new Address();
						populateAddress(fromAddress, startElement);
						order.setFrom(fromAddress);
					}
					if (qName.equalsIgnoreCase("to")) {
						//System.out.println("Start Element : to");
						toAddress = new Address();
						populateAddress(toAddress, startElement);
						order.setTo(toAddress);
					}
					if (qName.equalsIgnoreCase("line")) {
						//System.out.println("Start Element : line");
						lineItem = new LineItem();
						populateLineItem(lineItem, startElement);
						lineItems.add(lineItem);
					} 
					if (qName.equalsIgnoreCase("instructions")) {
						bInstructions = true;
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					Characters characters = event.asCharacters();
					if (bInstructions) {
						 instructions = characters.getData();
						order.setInstructions(instructions);
						bInstructions=false;
					}

					break;
				case XMLStreamConstants.END_ELEMENT:
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart().equalsIgnoreCase("line")) {
						//System.out.println("End Element : line");
					}
					if (endElement.getName().getLocalPart().equalsIgnoreCase("order")) {
						//System.out.println("End Element : order");

						order.setLines(lineItems);
						//System.out.println();
					}
					if (endElement.getName().getLocalPart().equalsIgnoreCase("instructions"))  {
					/*	
						System.out.println("instructions=* " + instructions);
						order.setInstructions(instructions);
					*/
					}
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException (e.getMessage());
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new XMLStreamException (e.getMessage());
		}

		//displayOrder(order);

		return order;

	}

	/*
	 * print the order that is processed in parsing operation
	 */
	private void displayOrder(Order order) {
		System.out.println("start output");
		System.out.println("==============================================");

		System.out.println("From City = " + order.getFrom().getCity());
		System.out.println("From State = " + order.getFrom().getState());
		System.out.println("From Zip = " + order.getFrom().getZip());

		System.out.println("To City = " + order.getTo().getCity());
		System.out.println("To State = " + order.getTo().getState());
		System.out.println("To Zip = " + order.getTo().getZip());

		List<LineItem> lineitems = order.getLines();
		for (LineItem temp : lineitems) {
			System.out.println("weight = " + temp.getWeight());
			System.out.println("getVolume = " + temp.getVolume());
			System.out.println("getHazard = " + temp.getHazard());
			System.out.println("getProduct = " + temp.getProduct());
		}

		System.out.println("instructions = " + order.getInstructions());
		System.out.println("==============================================");

		
	}

	
	/*
	 * populate the Address object by reading the xml
	 */

	private static void populateAddress(Address address, StartElement startElement) {
		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			QName qName = attribute.getName();
			String attrName = qName.getLocalPart();
			String value = attribute.getValue();
			value = value.trim();

			if (attrName.equalsIgnoreCase("city"))
				address.setCity(value);
			if (attrName.equalsIgnoreCase("zip"))
				address.setZip(value);
			if (attrName.equalsIgnoreCase("state"))
				address.setState(value);
		}

	}

	/*
	 * check if the xml input is valid
	 */
	public boolean validateXMLSchema(String xsdPath, String xmlPath) throws IOException{

		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			InputStream in = IOUtils.toInputStream(xmlString, "UTF-8");
			Schema schema = factory.newSchema(new File(xsdPath));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(in));
		} catch (IOException | SAXException e) {
			System.out.println("Exception: " + e.getMessage());
			throw new IOException("we are unable to process the input document. "
					+ "The input file does not comply with the schema.<br />"+e.getMessage());
		}
		return true;
	}
	
	/*
	 * populate the LineItem obj by reading the xml
	 */

	private static void populateLineItem(LineItem lineItem, StartElement startElement) {
		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			QName qName = attribute.getName();
			String attrName = qName.getLocalPart();
			String value = attribute.getValue();
			value = value.trim();

			if (attrName.equalsIgnoreCase("weight"))
				lineItem.setWeight(new Double(value));
			if (attrName.equalsIgnoreCase("volume"))
				lineItem.setVolume(new Double(value));
			if (attrName.equalsIgnoreCase("hazard"))
				lineItem.setHazard(new Boolean(value));
			if (attrName.equalsIgnoreCase("product"))
				lineItem.setProduct(value);
			//System.out.println(attrName + " = " + attribute.getValue());
		}

	}
}
