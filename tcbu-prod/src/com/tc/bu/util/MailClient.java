package com.tc.bu.util;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.util.ByteArrayDataSource;

public class MailClient {
	
	public static final String SYSTEM_SENDER = "no-reply@truconnect.com";
	public static final Recipient RECIPIENT_SUPPORT = new Recipient("TruConnect","Admin","","dta@telscape.net");

	//TSCP Mail Server
//	public static final String smtpHost = "209.127.162.26";
	//TruConnect Mail Server
	public static final String smtpHost = "209.127.161.5";
	Vector<Recipient> 	mRecipientList;
	Vector<Recipient>	bccList;
//	Logger			mLogger;
	
	// [start] constructors
	
	public MailClient() {
		mRecipientList = new Vector<Recipient>();
		bccList = new Vector<Recipient>();
		addBccRecipient(RECIPIENT_SUPPORT);
		Recipient recipient = new Recipient("Peter","Maas","","peter.maas@truconnect.com");
		addBccRecipient(recipient);
		
		recipient = new Recipient("Joseph","Holop","","jholop@telscape.net");
		addBccRecipient(recipient);
	}
	
	public MailClient( Recipient iRecipient ) {
		this();
		addRecipient(iRecipient);
	}
	
	// [end] constructors
	
	// [start] public methods
	
	public Vector<Recipient> getRecipientList() {
		return mRecipientList;
	}
	
	public void addRecipient(Recipient iRecipient) {
		mRecipientList.add(iRecipient);
	}
	
	public void addBccRecipient(Recipient recipient) {
		bccList.add(recipient);
	}
	
	public void removeRecipient(Recipient iRecipient) {
		mRecipientList.remove(iRecipient);
	}

	public void postMail( String subject, String message, String from ) {
		try {
			postMail(getRecipientList(),subject,message,from);
		} catch( MessagingException me ) {
			me.printStackTrace();
		}
	}
	
	public void postMail( Vector<Recipient> recipients, String subject, String message , String from) throws MessagingException {
	    boolean debug = false;

	     //Set the host smtp address
	     Properties props = new Properties();
	     props.put("mail.smtp.host", smtpHost);

	    // create some properties and get the default Session
	    Session session = Session.getDefaultInstance(props, null);
	    session.setDebug(debug);

	    // create a message
	    Message msg = new MimeMessage(session);

	    // set the from and to address
	    InternetAddress addressFrom = new InternetAddress(from);
	    try {
	    	addressFrom.setPersonal("TruConnect");
	    } catch(UnsupportedEncodingException ue_ex ) {
	    	ue_ex.printStackTrace();
	    }
	    msg.setFrom(addressFrom);
	    InternetAddress[] addressTo = new InternetAddress[recipients.size()]; 
	    for (int i = 0; i < recipients.size(); i++)
	    {
	        addressTo[i] = new InternetAddress(recipients.get(i).getEmailAddress());
	    }
	    msg.setRecipients(Message.RecipientType.TO, addressTo);
	    
	    InternetAddress[] addressBcc = new InternetAddress[bccList.size()];
	    for( int i = 0; i < bccList.size(); ++i ) {
	    	addressBcc[i] = new InternetAddress(bccList.get(i).getEmailAddress()); 
//	    	msg.setRecipient(RecipientType.BCC, bccAddressTo);
	    }
	    msg.setRecipients(Message.RecipientType.BCC, addressBcc);

	    // Optional : You can also set your custom headers in the Email if you Want
	    msg.addHeader("MyHeaderName", "myHeaderValue");

	    // Setting the Subject and Content Type
	    msg.setSubject(subject);
	    msg.setContent(message, "text/html");
	    try {
	    	Transport.send(msg);
	    } catch( SendFailedException send_ex ) {
	    	send_ex.printStackTrace();
			Address[] validAddressList = send_ex.getValidSentAddresses();
			Address[] invalidAddressList=send_ex.getInvalidAddresses();
			Vector<Recipient> validVectorAddressList = new Vector<Recipient>();
			if( validAddressList != null ) {
				for( int i = 0; i < validAddressList.length; ++i ) {
					Recipient lRecipient = new Recipient(null,null,null,validAddressList[i].toString());
					validVectorAddressList.add(lRecipient);
				}
				recipients = validVectorAddressList;
			} 
			if( invalidAddressList != null ) {
				for( int i = 0; i < invalidAddressList.length; ++i ) {
					recipients.remove(invalidAddressList[i].toString());
				}
	    	} 
			if( ( validAddressList == null || validAddressList.length == 0 ) && ( invalidAddressList == null || invalidAddressList.length == 0 ) ){
		    	send_ex.printStackTrace();
	    		throw send_ex;
	    	}
			if( validVectorAddressList == null || validVectorAddressList.isEmpty() ) {
				Recipient lRecipient = new Recipient(null,null,null,"dta@telscape.net");
				validVectorAddressList.add(lRecipient);
				String recipientList = "Exception Message :: "+send_ex.getMessage()+"\n\n";
				recipientList += "Unaltered Recipient List :: \n";
				for( int i = 0; i < recipients.size(); ++i ) {
					if( recipients.get(i) instanceof com.tc.bu.util.Recipient ) {
						recipientList += "        Recipient["+i+"].getEmailAddress() :: "+recipients.get(i).getEmailAddress()+"\n";
						
					}
				}
				if( validAddressList != null ) {
					recipientList += "\n\n";
					recipientList += "Valid Address List :: \n";
					for( int i = 0; i < validAddressList.length; ++i ) {
						lRecipient = new Recipient(null,null,null,validAddressList[i].toString());
						validVectorAddressList.add(lRecipient);
						recipientList += "        validAddressList["+i+"].toString() :: "+validAddressList[i].toString()+"\n";
					}
					recipients = validVectorAddressList;
				} 
				if( invalidAddressList != null ) {
					recipientList += "\n\n";
					recipientList += "Invalid Address List :: \n";
					for( int i = 0; i < invalidAddressList.length; ++i ) {
						recipients.remove(invalidAddressList[i].toString());
						recipientList += "        invalidAddressList["+i+"].toString() :: "+invalidAddressList[i].toString()+"\n";
					}
		    	} 
				postMail(validVectorAddressList,"Error Sending RPI Notification",recipientList,from);
			}
			postMail(validVectorAddressList,subject,message,from);
	    }
	}
	  
	public static void sendHTML(String from, Vector<String> toList, String subject, String body) throws Exception {
		    // Get system properties
		Properties props = System.getProperties();
		
		// Setup mail server
		props.put("mail.smtp.host", smtpHost);
		
		// Get session
		Session session = Session.getDefaultInstance(props, null);
		
		// Define message
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		for( int i = 0; i < toList.size(); ++i ) {
		    message.addRecipient(Message.RecipientType.TO, new InternetAddress(toList.get(i)));
		}
		message.setSubject(subject);
		message.setDataHandler(new DataHandler(new ByteArrayDataSource(body,"text/html")));
		//message.setText(body);
		
		// Send message
		try{ 
			Transport.send(message);
		} catch( SendFailedException a ) {
			a.printStackTrace();
			Address[] validAddressList = a.getValidSentAddresses();
			Address[] invalidAddressList=a.getInvalidAddresses();
			if( validAddressList != null ) {
				Vector<String> validVectorAddressList = new Vector<String>();
				for( int i = 0; i < validAddressList.length; ++i ) {
					validVectorAddressList.add(validAddressList[i].toString());
				}
				toList = validVectorAddressList;
			} else if( invalidAddressList != null ) {
				for( int i = 0; i < invalidAddressList.length; ++i ) {
					toList.remove(invalidAddressList[i].toString());
				}
			} else if( toList.isEmpty() ) {
				toList.add("omssupport@telscape.net");
			//		    		send(from,toList,subject,body);
	    	} else {
	    		throw a;
	    	}
	    	sendHTML(from,toList,subject,body);
	    } catch( Exception e ) {
	    	e.printStackTrace();
	    }
	}
	
	// [end] public methods

}
