package com.venu.develop.model;

import java.io.Serializable;


/*
 * When the database access is not set up, we need to implement serializable (marker interface)
 * for the object, 'Order' to be serialized and the data('Order Object graph') is saved to the hard disk.
 * 
 */

/**
 * @author Venu
 *
 */
@SuppressWarnings("serial")
public class LineItem implements Serializable{
    private Double weight; 
    private Double volume;
    private Boolean hazard = false;
    private String product;
    
    public LineItem(){}
    
    public LineItem(Double weight,    Double volume,  Boolean hazard, String product){
    	this.weight = weight;
    	this.volume= volume;
    	this.hazard= hazard;
    	this.product=product;
    }
	/**
	 * @return the weight
	 */
	public Double getWeight() {
		return weight;
	}
	/**
	 * @return the volume
	 */
	public Double getVolume() {
		return volume;
	}
	/**
	 * @return the hazard
	 */
	public Boolean getHazard() {
		return hazard;
	}
	/**
	 * @return the product
	 */
	public String getProduct() {
		return product;
	}
	/**
	 * @param weight the weight to set
	 */
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	/**
	 * @param volume the volume to set
	 */
	public void setVolume(Double volume) {
		this.volume = volume;
	}
	/**
	 * @param hazard the hazard to set
	 */
	public void setHazard(Boolean hazard) {
		this.hazard = hazard;
	}
	/**
	 * @param product the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	
	public String toString(){
		
		return "[" + this.product + "," + this.weight +"," + this.volume + "," + this.hazard  + "]";
	}	
}
