package com.tc.bu;

public class PaymentException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PaymentException() {
		super();
	}
	
	public PaymentException(String message) {
		super(message);
	}
	
	public PaymentException(Throwable t) {
		super(t);
	}
	
	public PaymentException(String message, Throwable t) {
		super(message,t);
	}

}
