package com.tc.bu;

public class ProcessException extends Exception {

	private String action;
	private String accountNo;
	private String mdn;

	private com.tscp.mvne.Account account;
	private com.tscp.mvne.NetworkInfo networkInfo;
	
	private String subject;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ProcessException(String action, String message) {
		super(message);
		setAction(action);
	}
	
	public ProcessException(String action, Throwable t) {
		super(t);
		setAction(action);
	}
	
	public ProcessException(String action, String message, Throwable t) {
		super(message,t);
		setAction(action);
	}
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getAccountNo() {
		return accountNo;
	}
	
	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}
	
	public String getMdn() {
		return mdn;
	}
	
	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	
	public com.tscp.mvne.Account getAccount() {
		return account;
	}
	
	public void setAccount(com.tscp.mvne.Account account) {
		this.account = account;
	}
	
	public com.tscp.mvne.NetworkInfo getNetworkInfo() {
		return networkInfo;
	}
	
	public void setNetworkInfo(com.tscp.mvne.NetworkInfo networkInfo) {
		this.networkInfo = networkInfo;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
}
