package com.tc.bu;

public class CustomerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CustomerException() {
		super();
	}
	
	public CustomerException(String message) {
		super(message);
	}
	
	public CustomerException(Throwable t) {
		super(t);
	}
	
	public CustomerException(String message, Throwable t) {
		super(message,t);
	}
	
}
