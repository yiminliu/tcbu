package com.tc.bu.dao;

public class CustBalance {

	private int accountNo;
	
	private double kenanBalance;
	private double proRatedAmount;
	private double dataCharges;
	private double totalCharges;
	private double realBalance;
	
	public CustBalance() {
		
	}

	public int getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(int accountNo) {
		this.accountNo = accountNo;
	}

	public double getKenanBalance() {
		return kenanBalance;
	}

	public void setKenanBalance(double kenanBalance) {
		this.kenanBalance = kenanBalance;
	}

	public double getProRatedAmount() {
		return proRatedAmount;
	}

	public void setProRatedAmount(double proRatedAmount) {
		this.proRatedAmount = proRatedAmount;
	}

	public double getDataCharges() {
		return dataCharges;
	}

	public void setDataCharges(double dataCharges) {
		this.dataCharges = dataCharges;
	}

	public double getTotalCharges() {
		return totalCharges;
	}

	public void setTotalCharges(double totalCharges) {
		this.totalCharges = totalCharges;
	}

	public double getRealBalance() {
		return realBalance;
	}

	public void setRealBalance(double realBalance) {
		this.realBalance = realBalance;
	}
	
	
	
}
