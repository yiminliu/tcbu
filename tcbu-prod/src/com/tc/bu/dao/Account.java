package com.tc.bu.dao;

public class Account {
	
	int accountno;
	String mdn;
	
	String realBalance;
	
	public Account() {
		
	}

	public int getAccountno() {
		return accountno;
	}

	public void setAccountno(int accountno) {
		this.accountno = accountno;
	}

	public String getMdn() {
		return mdn;
	}

	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	
	public String getRealBalance() {
		return realBalance;
	}
	
	public void setRealBalance(String realBalance) {
		this.realBalance = realBalance;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "AccountNo :: "+getAccountno()+" || MDN :: "+getMdn() + " || REAL_BALANCE :: "+getRealBalance();
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if( obj instanceof Account ) {
			if(	((Account) obj).getAccountno() == getAccountno()
					&& ((Account) obj).getMdn().equals(getMdn())
					&& ((Account) obj).getRealBalance().equals(getRealBalance())
					) {
				return true;
			} 
		}
		return super.equals(obj);
	}
}
