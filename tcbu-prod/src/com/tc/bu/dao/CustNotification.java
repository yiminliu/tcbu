package com.tc.bu.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.classic.Session;

import com.tc.bu.CustomerException;
import com.tc.bu.db.HibernateUtil;

public class CustNotification {

	private int notificationId;
	private String notificationType;
	
	private int custId;
	
	private int accountNo;
	
	private String mdn;
	private String esn;
	
	private Date createDate;
	private Date sentDate;
	
	private String to;
	private String cc;
	private String bcc;
	private String from;
	private String subject;
	private String body;
	
	public CustNotification() {
		notificationId = 0;
		custId = 0;
		accountNo = 0;
	}

	public int getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(int notificationId) {
		this.notificationId = notificationId;
	}

	public String getNotificationType() {
		return notificationType;
	}
	
	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}
	
	public int getCustId() {
		return custId;
	}

	public void setCustId(int custId) {
		this.custId = custId;
	}

	public int getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(int accountNo) {
		this.accountNo = accountNo;
	}

	public String getMdn() {
		return mdn;
	}

	public void setMdn(String mdn) {
		this.mdn = mdn;
	}

	public String getEsn() {
		return esn;
	}

	public void setEsn(String esn) {
		this.esn = esn;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}
	
	public String getCc() {
		return cc;
	}
	
	public void setCc(String cc) {
		this.cc = cc;
	}
	
	public String getBcc() {
		return bcc;
	}
	
	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	public void save() throws CustomerException {
		String queryName = "";
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		
		queryName = "upd_cust_notification";
				
		Query query = session.getNamedQuery(queryName);
		query.setParameter("notification_id", getNotificationId());
		query.setParameter("cust_id", getCustId());
		query.setParameter("account_no", getAccountNo());
		query.setParameter("mdn", getMdn());
		query.setParameter("esn", getEsn());
		query.setParameter("to", getTo());
		query.setParameter("cc", getCc());
		query.setParameter("bcc", getBcc());
		query.setParameter("from", getFrom());
		query.setParameter("subject", getSubject());
		query.setParameter("sent_date", getSentDate());
		List<GeneralSPResponse> responseList = query.list();
		if( responseList != null && responseList.size() > 0 ) {
			for( GeneralSPResponse generalResponse : responseList ) {
				if( !generalResponse.getStatus().equals("Y") ) {
					throw new CustomerException("Error saving notification..."+generalResponse.getMvnemsgcode()+"::"+generalResponse.getMvnemsg());
				}
			}
		}
		
		session.getTransaction().commit();
	}
	
}
