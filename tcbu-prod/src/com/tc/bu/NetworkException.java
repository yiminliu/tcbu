package com.tc.bu;

public class NetworkException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NetworkException() {
		super();
	}
	
	public NetworkException(String message) {
		super(message);
	}
	
	public NetworkException(Throwable t) {
		super(t);
	}
	
	public NetworkException(String message, Throwable t) {
		super(message,t);
	}

}
