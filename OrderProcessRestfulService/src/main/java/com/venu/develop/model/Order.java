package com.venu.develop.model;

import java.io.Serializable;
import java.util.List;

/*
 * When the database access is not set up, we need to implement serializable (marker interface)
 * for the object, 'Order' to be serialized and the data('Order Object graph') is saved to the hard disk.
 * 
 */

@SuppressWarnings("serial")
public class Order implements Serializable{

	//lombok-1.16.6.jar
	//@Getter @Setter @NonNull
	private Long id;
	private Address from;
	private Address to;
	private List<LineItem> lines;
	private String instructions;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the fromAddress
	 */
	public Address getFrom() {
		return from;
	}
	/**
	 * @param fromAddress the fromAddress to set
	 */
	public void setFrom(Address fromAddress) {
		this.from = fromAddress;
	}
	/**
	 * @return the toAddress
	 */
	public Address getTo() {
		return to;
	}
	/**
	 * @param toAddress the toAddress to set
	 */
	public void setTo(Address toAddress) {
		this.to = toAddress;
	}
	/**
	 * @return the instructions
	 */
	public String getInstructions() {
		return instructions;
	}
	/**
	 * @param instructions the instructions to set
	 */
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}
	/**
	 * @return the lineItem
	 */
	public List<LineItem> getLines() {
		return lines;
	}
	/**
	 * @param lineItem the lineItem to set
	 */
	public void setLines(List<LineItem> lineItem) {
		lines = lineItem;
	}
	
	public String toString(){
		
		 return "["+this.id + "," + this.from.toString() + "," + 
				 this.to. toString()  + "," + "[{" + this.getLines() + "}]" + "," + 
				 this.instructions +"]"; 
	}
}
