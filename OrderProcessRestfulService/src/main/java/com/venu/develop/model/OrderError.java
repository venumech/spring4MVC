package com.venu.develop.model;

public class OrderError {
	private Boolean error = false;
	private String errorMsg;
	
	/**
	 * @return the error
	 */
	public Boolean getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(Boolean error) {
		this.error = error;
	}
	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}
	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public String toString(){
		
		 return "["+this.error + "," + this.errorMsg  +"]"; 
	}
	
}
