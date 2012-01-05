package com.tc.bu.util;

public class Recipient {
	private String mFirstName;
	private String mLastName;
	private String mContactPhone;
	private String mEmailAddress;
	
	public Recipient() {
		this("","","","");
	}
	
	public Recipient( String iFirstName, String iLastName, String iContactPhone, String iEmailAddress ) {
		setFirstName(iFirstName);
		setLastName(iLastName);
		setContactPhone(iContactPhone);
		setEmailAddress(iEmailAddress);
	}
	
	public void setFirstName( String idx ) {  mFirstName = idx; }
	public void setLastName( String idx ) {  mLastName = idx; }
	public void setContactPhone( String idx ) {  mContactPhone = idx; }
	public void setEmailAddress( String idx ) {  mEmailAddress = idx; }

	public String getFirstName() { return mFirstName; }
	public String getLastName() { return mLastName; }
	public String getContactPhone() { return mContactPhone; }
	public String getEmailAddress() { return mEmailAddress; }

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		sb.append("FirstName :: "+getFirstName());
		sb.append("||");
		sb.append("LastName  :: "+getLastName());
		sb.append("||");
		sb.append("Contact Phone :: "+getContactPhone());
		sb.append("||");
		sb.append("EmailAddress :: "+getEmailAddress());
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if( obj instanceof Recipient ) {
			Recipient tempRecipient = (Recipient)obj;
			if( tempRecipient.getContactPhone().equals(getContactPhone()) 
					&& tempRecipient.getEmailAddress().equals(getEmailAddress())
					&& tempRecipient.getFirstName().equals(getFirstName())
					&& tempRecipient.getLastName().equals(getLastName())
			) {
				return true;
			}
		}
		return super.equals(obj);
	}

}
