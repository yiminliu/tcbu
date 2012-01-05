package com.tc.bu;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bu.dao.Account;
import com.tc.bu.dao.CustBalance;
import com.tc.bu.dao.CustNotification;
import com.tc.bu.db.HibernateUtil;
import com.tc.bu.util.MailClient;
import com.tc.bu.util.Recipient;
import com.tscp.mvne.CreditCard;
import com.tscp.mvne.CustPmtMap;
import com.tscp.mvne.CustTopUp;
import com.tscp.mvne.Customer;
import com.tscp.mvne.NetworkInfo;
import com.tscp.mvne.PaymentUnitResponse;
import com.tscp.mvne.ServiceInstance;
import com.tscp.mvne.TruConnect;
import com.tscp.mvne.TruConnectService;

public class TruConnectBackend {
	//Test Connection
//	String wsdlLocation = "http://uscael004:8080/TSCPMVNE/TruConnectService?WSDL";
	//Prod Connection
	//String wsdlLocation = "http://uscael001-vm5:8080/TSCPMVNE/TruConnectService?WSDL";
	private static final String wsdlLocation = "http://10.10.30.190:8080/TSCPMVNE/TruConnectService?WSDL";
	String namespaceURI = "http://mvne.tscp.com/"; 
	String localPart = "TruConnectService";
	
	public static final String SWITCH_STATUS_ACTIVE = "A";
	public static final String SWITCH_STATUS_SUSPENDED = "S";
	public static final String SWITCH_STATUS_DISCONNECTED = "C";

	final Logger logger = LoggerFactory.getLogger(TruConnectBackend.class);
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
	SimpleDateFormat legibleDate = new SimpleDateFormat("MM/dd/yyyy");

	TruConnectService service;
	TruConnect port;

	List<Account> chargeList;
	List<Account> suspendList;
	List<Account> restoreList;
	
	com.tscp.mvne.Account tscpMvneAccount;
	NetworkInfo networkInfo;
	Customer customer;

	public TruConnectBackend() {
	
		try {
			service = new TruConnectService(new URL(wsdlLocation), new QName(namespaceURI,localPart));
			port = service.getTruConnectPort();
		} catch ( MalformedURLException url_ex ) {
			
		}
	}
	
	private com.tscp.mvne.Account getTscpMvneAccount() {
		return tscpMvneAccount;
	}
	
	private void setTscpMvneAccount( com.tscp.mvne.Account tscpMvneAccount ) {
		this.tscpMvneAccount = tscpMvneAccount;
	}
	
	private NetworkInfo getNetworkInfo() {
		return networkInfo;
	}
	
	private void setNetworkInfo(NetworkInfo networkInfo) {
		this.networkInfo = networkInfo;
	}

	public void chargeAccounts() {
		// Get Account List to charge
		List<Account> accountList = getAccountToChargeList();

		for (Account account : accountList) {
			try {								
				if ( true /*account.getAccountno() == 693931*/ ) {
					tscpMvneAccount = new com.tscp.mvne.Account();
					networkInfo = new NetworkInfo();
					customer = new Customer();
				
					getCustomerInfo(account);
					try {
	
						// Get the First payment option
						logger.info("Getting first payment option for customer id "
								+ customer.getId());
						int defaultPaymentId = getCustomerPaymentDefault(customer);
	
						// Retrieve customer's top-up amount
						logger.info("Getting customer[" + customer.getId()
								+ "] top up amount ");
						CustTopUp topup = getCustomerTopUpAmount(customer, tscpMvneAccount);
						if (topup == null || topup.getTopupAmount().equals("")) {
							throw new CustomerException(
									"Top up amount has not been set for customer id "
											+ customer.getId() + " and account "
											+ account.getAccountno());
						}
						
						logger.info("Adding quantities to topUp to reconcile account");
						int topUpQuantity = 0;
						CustBalance currentBalance = getCustBalance(account.getAccountno());
						tscpMvneAccount.setBalance(Double.toString(currentBalance.getRealBalance()*-1));
						while( Double.parseDouble(tscpMvneAccount.getBalance()) < 2.0 ) {
							++topUpQuantity;
							tscpMvneAccount.setBalance(Double.toString(Double.parseDouble(tscpMvneAccount.getBalance())+Double.parseDouble(topup.getTopupAmount())));
						}
						Double chargeAmount = Double.parseDouble(topup.getTopupAmount())*topUpQuantity;
						logger.info("Customer will be topped up "+topUpQuantity+" times. Total Charge will be "+NumberFormat.getCurrencyInstance().format(chargeAmount));
						DecimalFormat df = new DecimalFormat("0.00");
						
						// Make the Payment for the customer using the custid and
						// pmt_id
						logger.info("Submitting Payment for Customer "
								+ customer.getId() + " against Account number "
								+ tscpMvneAccount.getAccountno() + " using PMT ID "
								+ defaultPaymentId
								+ " to be charged an amount of "
								+ df.format(chargeAmount));//topup.getTopupAmount());
						try {
							PaymentUnitResponse response = makePayment(customer,
									defaultPaymentId, tscpMvneAccount, null,
									df.format(chargeAmount));//topup.getTopupAmount());
	
							// send a notification of the results
							if (response != null) {
								logger.info("PaymentUnit Response ");
								logger.info("AuthCode   :: "+response.getAuthcode());
								logger.info("ConfCode   :: "+response.getConfcode());
								logger.info("ConfDescr  :: "+response.getConfdescr());
								logger.info("CvvCode    :: "+response.getCvvcode());
								logger.info("TransId    :: "+response.getTransid());
								
								/**
								 * "1.5" rule...Suspending service on payment failure
								 * this is handled on server side... 
								 */
//								if( !response.getConfcode().equals("0") ) {
//									logger.info("Transaction failed...Suspending account");
//									ServiceInstance serviceInstance = new ServiceInstance();
//									serviceInstance.setExternalid(account.getMdn());
//									port.suspendService(serviceInstance);
//									
//									EmailNotification emailNotification = new EmailNotification();
//									emailNotification.setTemplate(EmailTemplate.PAYMENT_FAILED);
//									port.sendNotification(customer, emailNotification);
//								}
//								if (response.getConfcode().equals("0")) {
//									logger.info("Sending confirmation notification to "
//											+ tscpMvneAccount.getContactEmail());
//									String confirmationNumber = "CC"
//											+ response.getConfcode() + "TID"
//											+ response.getTransid();
//									CreditCard paymentInfo = getPaymentInformation(
//											customer.getId(), defaultPaymentId);
//									String paymentMethod = "unknown";
//									String paymentSource = "";
//									if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("3")) {
//										paymentMethod = "AmericanExpress";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("4")) {
//										paymentMethod = "Visa";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("5")) {
//										paymentMethod = "MasterCard";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("6")) {
//										paymentMethod = "Discover";
//									}
////									tscpMvneAccount = port.getAccountInfo(account.getAccountno());
//									paymentSource = paymentInfo.getCreditCardNumber().substring(paymentInfo.getCreditCardNumber().length()-4, paymentInfo.getCreditCardNumber().length());
//									String body = getPaymentSuccessBody(
//											tscpMvneAccount.getFirstname(), /* userName */
//											Integer.toString(tscpMvneAccount.getAccountno()), /* accountNo */
//											account.getMdn(),/*MDN*/
//											networkInfo.getEsnmeiddec(), /*ESN*/
//											confirmationNumber, /* confirmationNumber */
//											topup.getTopupAmount(), /* Pmt Amount */
//											paymentMethod, /* paymentMethod */
//											paymentSource, /* Pmt Source */
//											legibleDate.format(new Date()) /* pmt date */
//											,tscpMvneAccount.getBalance());
//									String subject = "A payment has been posted for Account "+ tscpMvneAccount.getAccountno();
//									sendEmail(tscpMvneAccount.getContactEmail(), subject, body);
//									logger.info("Email has been issued...");
//								} else {
//									logger.info("Sending failure notification to "
//											+ tscpMvneAccount.getContactEmail());
//									String remainingBalance = tscpMvneAccount.getBalance();
//									CreditCard paymentInfo = getPaymentInformation(
//											customer.getId(), defaultPaymentId);
//									String paymentMethod = "unknown";
//									String paymentSource = "";
//									if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("3")) {
//										paymentMethod = "AmericanExpress";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("4")) {
//										paymentMethod = "Visa";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("5")) {
//										paymentMethod = "MasterCard";
//									} else if (paymentInfo.getCreditCardNumber()
//											.substring(0, 1).equals("6")) {
//										paymentMethod = "Discover";
//									}
//									paymentSource = paymentInfo.getCreditCardNumber().substring(paymentInfo.getCreditCardNumber().length()-4, paymentInfo.getCreditCardNumber().length());
//									String body = getPaymentFailureBody(
//											tscpMvneAccount.getFirstname(),
//											Integer.toString(tscpMvneAccount.getAccountno()),
//											account.getMdn(), /*MDN*/
//											networkInfo.getEsnmeiddec(), /*ESN*/
//											topup.getTopupAmount(), /* Pmt Amount */
//											paymentMethod, /* Pmt Method */
//											paymentSource, /*
//																				 * Pmt
//																				 * Source
//																				 */
//											legibleDate.format(new Date()), /*
//																			 * Pmt
//																			 * Date
//																			 */
//											"CC" + response.getConfcode() + "TID"
//													+ response.getTransid() + "::"
//													+ response.getConfdescr() + "|"
//													+ response.getAuthcode(), /* Comment */
//											remainingBalance);
//									sendEmail(
//											tscpMvneAccount.getContactEmail(),
//											"Error processing payment for Account "
//													+ tscpMvneAccount
//															.getAccountno(), body);
//									logger.info("Email has been issued...");
//								}
							} else {
								logger.info("Sending null response notification");
							}
						} catch (PaymentException payment_ex) {
							logger.info("Sending failure notification to "+"dta@telscape.net");
//									+ tscpMvneAccount.getContactEmail());
							String remainingBalance = tscpMvneAccount.getBalance();
							CreditCard paymentInfo = getPaymentInformation(
									customer.getId(), defaultPaymentId);
							String paymentMethod = "unknown";
							if (paymentInfo.getCreditCardNumber().substring(0, 1)
									.equals("3")) {
								paymentMethod = "AmericanExpress";
							} else if (paymentInfo.getCreditCardNumber()
									.substring(0, 1).equals("4")) {
								paymentMethod = "Visa";
							} else if (paymentInfo.getCreditCardNumber()
									.substring(0, 1).equals("5")) {
								paymentMethod = "MasterCard";
							} else if (paymentInfo.getCreditCardNumber()
									.substring(0, 1).equals("6")) {
								paymentMethod = "Discover";
							}
							String body = getPaymentFailureBody(
									tscpMvneAccount.getFirstname(),
									Integer.toString(tscpMvneAccount.getAccountno()),
									account.getMdn(), /*MDN*/
									""/*networkInfo.getEsnmeiddec()*/, /*ESN*/
									topup.getTopupAmount(), /* Pmt Amount */
									paymentMethod, /* Pmt Method */
									paymentInfo.getCreditCardNumber(), /*
																		 * Pmt
																		 * Source
																		 */
									legibleDate.format(new Date()), /* Pmt Date */
									payment_ex.getMessage(), /* Comment */
									remainingBalance);
							sendEmail("dta@telscape.net",//tscpMvneAccount.getContactEmail(),
									"Error processing payment for Account "
											+ tscpMvneAccount.getAccountno(), body);
							logger.info("Email has been issued...");
						}
						logger.info("Done with Account " + account.getAccountno());
					} catch (CustomerException cust_ex) {
						logger.warn("Customer Exception thrown :: "+ cust_ex.getMessage()+"...Skipping Account "+account.getAccountno(), cust_ex);
						ProcessException process_ex = new ProcessException("Payment Processing",cust_ex.getMessage(),cust_ex);
						process_ex.setAccountNo(Integer.toString(tscpMvneAccount.getAccountno()));
						process_ex.setMdn(account.getMdn());
						process_ex.setAccount(tscpMvneAccount);
						process_ex.setNetworkInfo(networkInfo);
						throw process_ex;
					}
				}
			} catch( ProcessException process_ex ) {
				logger.info("Formerly we sent emails here...server is handling this action now");
//				if( process_ex.getAccount() == null || process_ex.getAccount().getContactEmail() == null ) {
//					if( process_ex.getAccount() == null ) {
//						process_ex.setAccount(new com.tscp.mvne.Account());
//					}
//					process_ex.getAccount().setContactEmail("support@truconnect.com");
//				}
//				if( process_ex.getSubject() == null ) {
//					process_ex.setSubject("Payment Processing Exception Error");
//				}
//				if( process_ex.getAccount().getFirstname() == null ) {
//					process_ex.getAccount().setFirstname("TruConnect Support Team");
//				}
//				String body = getErrorBody(process_ex);
//				sendEmail(process_ex.getAccount().getContactEmail(), process_ex.getSubject(), body);
			}
		}
	}

	public void hotlineAccounts() {
		logger.info("Retrieving accounts to hotline");
		//get list of accounts to hotline
		List<Account> accountList = getAccountsToHotLineList();

		if( accountList != null ) {
			for( Account account : accountList ) {
				
				try {				
					tscpMvneAccount = new com.tscp.mvne.Account();
					networkInfo = new NetworkInfo();
					
					getCustomerInfo(account);
					
					if( true/*account.getAccountno() == 681941*/ ) {
						try {
							if( networkInfo.getStatus().equals(SWITCH_STATUS_ACTIVE) ) {
								//submit hotline request
								ServiceInstance serviceInstance = new ServiceInstance();
								serviceInstance.setExternalid(account.getMdn());
								try {
									port.suspendService(serviceInstance);
								} catch( WebServiceException ws_ex ) {
									logger.warn("WebService Exception thrown when suspending MDN "+account.getMdn());
									logger.warn("Error: "+ws_ex.getMessage());
									if( ws_ex.getMessage().indexOf("does not exist") > 0 ) {
										throw new CustomerException("MDN "+serviceInstance.getExternalid()+"is currently not active and was not suspended..." );
									} else {
										throw new CustomerException(ws_ex.getMessage(),ws_ex.getCause());
									}
								}
							} else if( networkInfo.getStatus().equals(SWITCH_STATUS_SUSPENDED) ) {
								logger.info("MDN "+account.getMdn()+" skipped suspend because it is already in suspend status ");
							} else {
		//						logger.info("MDN "+account.getMdn()+" skipped suspend because it is currently in status "+networkInfo.getStatus());
								throw new CustomerException("MDN "+account.getMdn()+" was not suspended because it is currently in status "+networkInfo.getStatus());
							}
							
							//send notification
							logger.info("notifications are no longer sent through this medium");
//							String body = getSuspendedAccountNotification(tscpMvneAccount.getFirstname(),Integer.toString(tscpMvneAccount.getAccountno()),account.getMdn(),networkInfo.getEsnmeiddec(),legibleDate.format(new Date()));
//							sendEmail(tscpMvneAccount.getContactEmail(),"Your Account "+tscpMvneAccount.getAccountno()+" has been suspended",body);
							logger.info("Account "+account.getAccountno()+" and Mdn "+account.getMdn()+" has been suspended");
							
						} catch( WebServiceException ws_ex ) {
							logger.warn("WebService Exception thrown when getting networkInfo for MDN "+account.getMdn());
							logger.warn("Error: "+ws_ex.getMessage());
							ProcessException process_ex = new ProcessException("Account Hotline Processing",ws_ex.getMessage(),ws_ex);
							process_ex.setAccountNo(Integer.toString(tscpMvneAccount.getAccountno()));
							process_ex.setMdn(account.getMdn());
							process_ex.setNetworkInfo(networkInfo);
							process_ex.setAccount(tscpMvneAccount);
							throw process_ex;
						} catch( CustomerException customer_ex ) {
							logger.warn("CustomerException thrown :: "+customer_ex.getMessage()+"...skipping account "+account.getAccountno());
							ProcessException process_ex = new ProcessException("Account Hotline Processing",customer_ex.getMessage(),customer_ex);
							process_ex.setAccountNo(Integer.toString(tscpMvneAccount.getAccountno()));
							process_ex.setMdn(account.getMdn());
							process_ex.setNetworkInfo(networkInfo);
							process_ex.setAccount(tscpMvneAccount);
							throw process_ex;
						}
					}
				} catch( ProcessException process_ex ) {
					if( process_ex.getSubject() == null ) {
						process_ex.setSubject("Hotline Processing Exception Error");
					}
					if( process_ex.getAccount() == null ) {
						process_ex.setAccount(new com.tscp.mvne.Account());
					}
					process_ex.getAccount().setFirstname("TruConnect Support Team");
					process_ex.getAccount().setContactEmail("dta@telscape.net");
					String body = getErrorBody(process_ex.getAccount().getFirstname(),process_ex.getAccountNo(),process_ex.getMdn(),process_ex.getNetworkInfo().getEsnmeiddec(),process_ex.getAction(),process_ex.getMessage());
					sendEmail(process_ex.getAccount().getContactEmail(), process_ex.getSubject(), body);
				}
			}
			
		} else {
			logger.info("No accounts to hotline");
		}
	}
	
	public void restoreAccounts() {
		//get list of accounts to restore
		List<Account> accountList = getAccountsToRestoreList();

		if( accountList != null ) {
			for( Account account : accountList ) {
				try {
					tscpMvneAccount = new com.tscp.mvne.Account();
					networkInfo = new NetworkInfo();
					
					getCustomerInfo(account);
					
					if( true/*account.getAccountno() == 681789*/ ) {
						try {
							if( networkInfo.getStatus().equals(SWITCH_STATUS_SUSPENDED) ) {
								//submit restore request
								ServiceInstance serviceInstance = new ServiceInstance();
								serviceInstance.setExternalid(account.getMdn());
								try {
									port.restoreService(serviceInstance);
								} catch( WebServiceException ws_ex ) {
									logger.warn("WebService Exception thrown when restoring MDN "+account.getMdn());
									logger.warn("Error: "+ws_ex.getMessage());
									if( ws_ex.getMessage().indexOf("does not exist") > 0 ) {
										throw new CustomerException("MDN "+serviceInstance.getExternalid()+"is currently not active and was not restored..." );
									} else {
										throw new CustomerException(ws_ex.getMessage(),ws_ex.getCause());
									}
								}
							} else if( networkInfo.getStatus().equals(SWITCH_STATUS_ACTIVE) ) {
								throw new CustomerException("Account "+account.getAccountno()+" is in the list to be restored however service is already in restored state");
							} else {
		//						logger.info("MDN "+account.getMdn()+" skipped suspend because it is currently in status "+networkInfo.getStatus());
								throw new CustomerException("MDN "+account.getMdn()+" was not restored because it is currently in status "+networkInfo.getStatus());
							}
							
							//send notification
							logger.info("notifications are no longer sent through this medium");
//							String body = getRestoredAccountNotification(tscpMvneAccount.getFirstname(),Integer.toString(tscpMvneAccount.getAccountno()),account.getMdn(),networkInfo.getEsnmeiddec(),legibleDate.format(new Date()));
//							sendEmail(tscpMvneAccount.getContactEmail(),"Your Account "+tscpMvneAccount.getAccountno()+" has been Restored",body);
							logger.info("Account "+account.getAccountno()+" and Mdn "+account.getMdn()+" has been restored");
							
						} catch( WebServiceException ws_ex ) {
							logger.warn("WebService Exception thrown when getting networkInfo for MDN "+account.getMdn());
							logger.warn("Error: "+ws_ex.getMessage());
							ProcessException process_ex = new ProcessException("Account Restore Processing",ws_ex.getMessage(),ws_ex);
							process_ex.setAccountNo(Integer.toString(tscpMvneAccount.getAccountno()));
							process_ex.setMdn(account.getMdn());
							process_ex.setNetworkInfo(networkInfo);
							process_ex.setAccount(tscpMvneAccount);
							throw process_ex;
						} catch( CustomerException customer_ex ) {
							logger.warn("CustomerException thrown :: "+customer_ex.getMessage()+"...skipping account "+account.getAccountno());
							ProcessException process_ex = new ProcessException("Account Restore Processing",customer_ex.getMessage(),customer_ex);
							process_ex.setAccountNo(Integer.toString(tscpMvneAccount.getAccountno()));
							process_ex.setMdn(account.getMdn());
							process_ex.setNetworkInfo(networkInfo);
							process_ex.setAccount(tscpMvneAccount);
							throw process_ex;
						}
					}
				} catch( ProcessException process_ex ) {
					if( process_ex.getSubject() == null ) {
						process_ex.setSubject("Hotline Processing Exception Error");
					}
					if( process_ex.getAccount() == null ) {
						process_ex.setAccount(new com.tscp.mvne.Account());
					}
					process_ex.getAccount().setFirstname("TruConnect Support Team");
					process_ex.getAccount().setContactEmail("dta@telscape.net");
					String body = getErrorBody(process_ex.getAccount().getFirstname(),process_ex.getAccountNo(),process_ex.getMdn(),process_ex.getNetworkInfo().getEsnmeiddec(),process_ex.getAction(),process_ex.getMessage());
					sendEmail(process_ex.getAccount().getContactEmail(), process_ex.getSubject(), body);
					CustNotification notification = new CustNotification();
					notification.setAccountNo(Integer.parseInt(process_ex.getAccountNo()));
					notification.setMdn(process_ex.getMdn());
					notification.setEsn(process_ex.getNetworkInfo().getEsnmeiddec());
//					notification.setCustId(process_ex.getCustId());
					notification.setBody(body);
				}
			}
			
		} else {
			logger.info("No accounts to hotline");
		}
	}

	private void getCustomerInfo(Account account) throws ProcessException {
		// TODO Auto-generated method stub

		ProcessException process_ex = null;
		try {
			// Get Customer from the Account list
			logger.info("Getting Customer Information for Account " + account.getAccountno());
			customer = getCustomerFromAccount(account.getAccountno());
			if (customer.getId() == 0) {
				throw new CustomerException("Customer could not be located for account number " + account.getAccountno());
			}
		} catch( CustomerException cust_ex ) {
			process_ex = new ProcessException("CustomerInformation Retrieval",cust_ex);
		}
		
		try {
			// Create TSCPMVNE Account Object
			logger.info("Creating TSCP MVNE Account Object");
			tscpMvneAccount = getAccountInfo(account.getAccountno());
		} catch( CustomerException cust_ex ) {
			if( process_ex == null ) {
				process_ex = new ProcessException("AccountInformation Retrieval",cust_ex);
			}
		} 
//		try {
//			// Get NetworkInformation 
//			logger.info("Gathering network information");
//			networkInfo = getNetworkInfo(account);
//		} catch( NetworkException network_ex ) {
//			if( process_ex == null ) {
//				process_ex = new ProcessException("DeviceInformation Retrieval",network_ex);
//			}
//		}
		if( process_ex != null ) {
			process_ex.setAccountNo(Integer.toString(account.getAccountno()));
			process_ex.setMdn(account.getMdn());
			process_ex.setAccount(tscpMvneAccount);
			process_ex.setNetworkInfo(networkInfo);
			throw process_ex;
		}
	}
	
	private CreditCard getPaymentInformation(int custid, int pmtid) throws CustomerException {
//		List<CustPmtMap> custPmtMapList = port.getCustPaymentList(custid, pmtid);
		CreditCard creditCard = port.getCreditCardDetail(pmtid);
		if( creditCard == null || creditCard.getCreditCardNumber() == null || creditCard.getCreditCardNumber().trim().length() <= 0 ) {
			throw new CustomerException("Error retrieving creditcard information for customer "+custid);
		}
		return creditCard;
	}

	private com.tscp.mvne.Account getAccountInfo(int accountNo) throws CustomerException {
		com.tscp.mvne.Account account = port.getAccountInfo(accountNo);
		if( account == null ) {
			throw new CustomerException("Unable to get Account Information from billing system for customer mapped account "+accountNo);			
		} else if(account.getContactEmail() == null || account.getContactEmail().trim().length() == 0) {
			throw new CustomerException("Unable to get Customer Email Address for issuing notifications on account "+account.getAccountno());
		}
		return account;
	}

	private CustTopUp getCustomerTopUpAmount(Customer customer, com.tscp.mvne.Account tscpMvneAccount) throws CustomerException {
		CustTopUp ctu = new CustTopUp();
		ctu = port.getCustTopUpAmount(customer,tscpMvneAccount);
		if( ctu == null || ctu.getTopupAmount() == null || ctu.getTopupAmount().trim().length() <= 0 ) {
			throw new CustomerException("Customer topup amount has not been set");
		}
		return ctu;
	}

	private NetworkInfo getNetworkInfo(Account account) throws NetworkException {
		NetworkInfo networkInfo = port.getNetworkInfo(null, account.getMdn());
		if( networkInfo == null || networkInfo.getEsnmeiddec() == null || networkInfo.getEsnmeiddec().trim().length() <= 0 ) {
			throw new NetworkException("Unable to get device information for TN "+account.getMdn());
		}
		return networkInfo;
	}
	
	private int getCustomerPaymentDefault(Customer customer)
			throws CustomerException {
		// PaymentInformation paymentInfo = new PaymentInformation();
		int paymentId = 0;
		List<CustPmtMap> custPaymentMap = port.getCustPaymentList(
				customer.getId(), 0);
		if (custPaymentMap != null && custPaymentMap.size() > 0) {
			paymentId = custPaymentMap.get(0).getPaymentid();
		} else {
			throw new CustomerException(
					"Error retrieving Payments for Customer "
							+ customer.getId());
		}
		return paymentId;
	}

	private Customer getCustomerFromAccount(int accountno) throws ProcessException {
		Customer customer = new Customer();
		try {
			customer.setId(port.getCustFromAccount(accountno).getCustId());
		} catch( NullPointerException np_ex ) {
			logger.info(np_ex.getMessage());
			throw new ProcessException("CustomerRetrieval","Unable to get customer information from map against account "+accountno);
		}
		if( customer.getId() == 0 ) {
			logger.info("Customer information from map against account "+accountno+" returned a 0 CustID");
			throw new ProcessException("CustomerRetrieval","Customer information from map against account "+accountno+" returned a 0 CustID");
		}
		return customer;
	}

	private PaymentUnitResponse makePayment(Customer customer, int paymentId,
			com.tscp.mvne.Account account, CreditCard creditCard, String amount)
			throws PaymentException {
		logger.info("Making payment for CustomerId " + customer.getId()
				+ " against Pmt ID " + paymentId + " in the Amount of $"
				+ amount + ".");
		String sessionid = "CID" + customer.getId() + "T" + getTimeStamp()
				+ "AUTO";
		try {
			PaymentUnitResponse response = port.submitPaymentByPaymentId(
					sessionid, customer, paymentId, account, amount);
			return response;
		} catch (WebServiceException wse) {
			logger.warn("WebService Exception thrown :: " + wse.getMessage());
			//will catch this exception at main()
			if(wse.getMessage().indexOf("Attempted to read or write protected memory") >= 0){
				throw wse;
			}
//			wse.printStackTrace();
			if (wse.getCause() != null) {
				logger.warn("Immediate WSException Cause was :: "
						+ wse.getCause().getMessage());
			}
			throw new PaymentException(wse.getMessage());
		
		}
	}

	private List<Account> getAccountToChargeList() {
		logger.info("Fetching Accounts to Charge List....");
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();

		Query q = session.getNamedQuery("sp_fetch_accts_to_charge");
		List<Account> accountList = q.list();
		logger.info("Logger has returned with {} elements.", accountList.size());
//		for (Account account : accountList) {
//			logger.info(account.toString());
//		}

		session.getTransaction().commit();
		return accountList;
	}

	private List<Account> getAccountsToHotLineList() {
		logger.info("Fetching Accounts to Hotline List....");
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();

		Query q = session.getNamedQuery("sp_fetch_accts_to_hotline");
		List<Account> accountList = q.list();
		logger.info("Logger has returned with {} elements.", accountList.size());
		for (Account account : accountList) {
			logger.info(account.toString());
		}

		session.getTransaction().commit();
		return accountList;
	}

	private List<Account> getAccountsToRestoreList() {
		logger.info("Fetching Accounts to Restore List....");
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();

		Query q = session.getNamedQuery("sp_fetch_accts_to_restore");
		List<Account> accountList = q.list();
		logger.info("Logger has returned with {} elements.", accountList.size());
		for (Account account : accountList) {
			logger.info(account.toString());
		}

		session.getTransaction().commit();
		return accountList;
	}

	public void testPmtFail() {
		System.out.println("Testing Fail PMT ");
		try {
			PaymentUnitResponse pur = port.makeCreditCardPayment(null, null,
					null, null);
			System.out.println("AuthCode :: " + pur.getAuthcode());
		} catch (WebServiceException wse) {
			System.out.println(wse.getMessage());
		}
	}

	public void sendEmail(String emailAddress, String subject, String body) {
		MailClient mail = new MailClient();
		Vector<Recipient> recipients = new Vector<Recipient>();
		Recipient recipient = new Recipient();
		// recipient.setEmailAddress("omssupport@telscape.net");
		recipient.setEmailAddress(emailAddress);
		recipients.add(recipient);
		try {
			// MailClient.sendHTML(MailClient.SYSTEM_SENDER, toList,
			// "Hello World", "testing");

			body = getEmailHeader() + body + getEmailFooter();
			mail.postMail(recipients, subject, body, MailClient.SYSTEM_SENDER);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		TruConnectBackend tcb = new TruConnectBackend();
		System.out.println("Time Stamp :: " + tcb.getTimeStamp());
		// List<Account> chargeList = tcb.getAccountToChargeList();
		// for(Account account : chargeList ) {
		// Customer customer =
		// tcb.getCustomerFromAccount(account.getAccountno());
		// if( customer != null ) {
		// System.out.println(customer.toString());
		// System.out.println("Cust ID :: "+customer.getId());
		// }
		// }
		// tcb.getAccountsToHotLineList();
		// tcb.getAccountsToRestoreList();
		try{
		   tcb.chargeAccounts();
		}
        catch(WebServiceException wsException) {
           if(wsException.getMessage().indexOf("Attempted to read or write protected memory") >=0) {
              System.out.println("Momery corruptted, exits the process"); 
        	  System.exit(1);
           }  
        }   
//		tcb.hotlineAccounts();
//		tcb.restoreAccounts();
//		 tcb.testPmtFail();
		if( args != null && args.length > 0 && args[0].equals("emailtest") ) {
			System.out.println("Email body:");
			System.out.println(tcb.getEmailHeader());
			String body = tcb.getPaymentSuccessBody("Dan","681685","9099872481","01199411623","CC0TID1479","10","Discover","6XXX-XXXX-XXXX-0019","5/19/2011","50.00");
			System.out.println(body);
			System.out.println(tcb.getEmailFooter());
			tcb.sendEmail("dta@telscape.net","Payment processed for account 681685",body);
		}

	}

	private String getTimeStamp() {
		return sdf.format(new Date());
	}

	private String getEmailHeader() {
		StringBuffer header = new StringBuffer();

		header.append("<html>\n");
		header.append("<body>\n");
		header.append("    <div style=\"width:750px; margin:0px auto;\" \n");
		header.append("    <a href='https://manage.truconnect.com/TruConnect/login'> \n");
		header.append("    <img class='logo' src='https://activate.truconnect.com/TruConnect/static/images/logo_s1.jpg'> \n");
		header.append("    </a> \n");
		header.append("    <br>&nbsp;</br> \n");

		return header.toString();
	}

	private String getEmailFooter() {

		StringBuffer footer = new StringBuffer();

		footer.append("    <div style=\"background:#EFEFEF \"> \n");
		footer.append("    <p> \n");
		footer.append("        Sincerely, \n");
		footer.append("    <br> \n");
		footer.append("    <b>TruConnect</b> \n");
		footer.append("    </p> \n");
		footer.append("    </div> \n");
		footer.append("    <br> \n");
		footer.append("    <div style=\"border-top:1px solid #999; margin-bottom:15px; font-size:15px;\"> \n");
		footer.append("    <br> \n");
		footer.append("    <div style=\"color:#6D7B8D; font-size:0.8em;\"> \n");
		footer.append("        <b>Please Do Not Reply to this Message</b> \n");
		footer.append("        <br>All replies are autmatically deleted. For questions regarding this message, refer to the contact information listed above.</br> \n");
		footer.append("        <p><a href=\"www.truconnect.com\">&copy2011 TruConnect Intellectual Property.</a> All rights reserved. TruConnect, the TruConnect logo and all other TruConnect marks contained herein are trademarks of TruConnect Intellectual Property and/or TruConnect affiliated companies. Subsidiaries and affiliates of TruConnect LLC provide products and services under the TruConnect Brand</p> \n");
		footer.append("        <p><a href=\"www.truconnect.com\">Privacy Policy</a></p> \n");
		footer.append("        <p>Questions? Please visit the <a href=\"www.truconnect.com/support\">Support Page</a></p> \n");
		footer.append("    </div> \n");
		footer.append("</div> \n");
		footer.append("</body> \n");
		footer.append("</html> \n");

		return footer.toString();
	}

	private String getPaymentSuccessBody(String userName, String accountNo, String tn, String esn,
			String confirmationNumber, String amount, String paymentMethod,
			String paymentSource, String paymentDate, String balance) {
		StringBuffer body = new StringBuffer();
		body.append("<div style=\"color:#2554C7; font-size:1.25em; background:#EAEAEA\"> \n");
		body.append("<b>Your TruConnect Payment Processed</b> \n");
		body.append("</div> \n");
		body.append("<p> \n");
		body.append("	<b>Dear " + userName + ",</b> \n");
		body.append("</p> \n");
		body.append("<p>Thank you for your payment. Your payment has been successfully processed and will be noted immediately to your account. Below you will find the transaction information regarding your payment.</p> \n");
		body.append("");
		body.append("<p> \n");
		body.append("	<table border=1> \n");
		body.append("		<th>Account</th> \n");
		body.append("       <th>TN</th>");
		body.append("       <th>ESN</th>");
		body.append("		<th>Confirmation Number</th> \n");
		body.append("		<th>Amount</th> \n");
		body.append("		<th>Payment Method</th> \n");
		body.append("		<th>Payment Source</th> \n");
		body.append("		<th>Payment Date</th> \n");
		body.append("		<tr>");
		body.append("			<td>" + accountNo + "</td> \n");
		body.append("			<td>" + tn + "</td> \n");
		body.append("			<td>" + esn + "</td> \n");
		body.append("			<td>" + confirmationNumber + "</td> \n");
		body.append("			<td>$" + amount + "</td> \n");
		body.append("			<td>" + paymentMethod + "</td> \n");
		body.append("			<td>" + paymentSource + "</td> \n");
		body.append("			<td>" + paymentDate + "</td> \n");
		body.append("		</tr> \n");
		body.append("	</table> \n");
		body.append("</p> \n");
		body.append("");
		body.append("<p> \n");
		body.append("	<a href=\"https://manage.truconnect.com/truconnect/login\">Log in</a> and manage your billing and payment information \n");
		body.append("</p> \n");
		body.append("<p> \n");
		body.append("	Thank you for choosing TruConnect for your wireless and data needs. We value your business and look forward to serving you! \n");
		body.append("</p> \n");
		return body.toString();
	}

	private String getPaymentFailureBody(String userName, String accountNumber, String tn, String esn,
			String paymentAmount, String paymentMethod, String paymentSource,
			String paymentDate, String comments, String remainingBalance) {
		StringBuffer body = new StringBuffer();
		body.append("<div style=\"color:#306EFF; font-size:1.25em; background:#EAEAEA\">\n ");
		body.append("<b>Your TruConnect Payment Failed to Process</b>\n ");
		body.append("</div>\n ");
		body.append("<p>\n ");
		body.append("	<b>Dear " + userName + ",</b>\n ");
		body.append("</p>\n ");
		body.append("<p>Your payment has encountered issues when attempting to top up funds to your account. Below you will find the transaction information regarding your attempted payment.</p>\n ");
		body.append("\n ");
		body.append("<p>\n ");
		body.append("	<table border=1>\n ");
		body.append("		<th>Account</th>\n ");
		body.append("		<th>TN</th>\n ");
		body.append("		<th>ESN</th>\n ");
		body.append("		<th>Amount</th>\n ");
		body.append("		<th>Payment Method</th>\n ");
		body.append("		<th>Payment Source</th>\n ");
		body.append("		<th>Payment Date</th>\n ");
		body.append("		<th>Comments</th>\n ");
		body.append("		<tr>\n ");
		body.append("			<td>" + accountNumber + "</td>\n ");
		body.append("			<td>" + tn + "</td> \n");
		body.append("			<td>" + esn + "</td> \n");
		body.append("			<td>$" + paymentAmount + "</td>\n ");
		body.append("			<td>" + paymentMethod + "</td>\n ");
		body.append("			<td>" + paymentSource + "</td>\n ");
		body.append("			<td>" + paymentDate + "</td>\n ");
		body.append("			<td>" + comments + "</td>\n ");
		body.append("		</tr>\n ");
		body.append("	</table>\n ");
		body.append("</p>\n ");
		body.append("\n ");
		body.append("<p>\n ");
		body.append("	<a href=\"https://manage.truconnect.com/truconnect/login\">Log in</a> and manage your billing and payment information\n ");
		body.append("</p>\n ");
		body.append("<p>\n ");
		body.append("	Please make any necessary modifications to your payment information and add funds to your account to avoid service interruption. Your remaining balance is <b>"+remainingBalance+"</b> ");
		body.append("</p>\n ");
		return body.toString();
	}

	private String getSuspendedAccountNotification(
			String userName,
			String accountno,
			String mdn,
			String esn,
			String suspendDate
			) {
		StringBuffer body = new StringBuffer();
		body.append("<div style=\"color:#306EFF; font-size:1.25em; background:#EAEAEA\"> \n");
		body.append("<b>Your TruConnect Service has been Suspended</b> \n");
		body.append("</div> \n");
		body.append("<p> \n");
		body.append("	<b>Dear " + userName + ",</b> \n");
		body.append("</p> \n");
		body.append("<p>Services associated with your account " + accountno + " have been temporarily suspended due to lack of funds. Please add more funds your account inorder to restore service. Below you will find the device information regarding this suspension.</p> \n");
		body.append(" \n");
		body.append("<p> \n");
		body.append("	<table border=\"1\"> \n");
		body.append("		<th>Account</th> \n");
		body.append("		<th>TN</th> \n");
		body.append("		<th>ESN</th> \n");
		body.append("		<th>Suspend Date</th> \n");
		body.append("		<tr> \n");
		body.append("			<td>" + accountno + "</td> \n");
		body.append("			<td>" + mdn + "</td> \n");
		body.append("			<td>" + esn + "</td> \n");
		body.append("			<td>" + suspendDate + "</td> \n");
		body.append("		</tr> \n");
		body.append("	</table> \n");
		body.append("</p> \n");
		body.append(" \n");
		body.append("<p> \n");
		body.append("	<a href=\"https://manage.truconnect.com/truconnect/login\">Log in</a> and manage your billing and payment information \n");
		body.append("</p> \n");
		body.append("<p> \n");
		body.append("	Thank you for choosing TruConnect for your wireless and data needs. We value your business and look forward to serving you! \n");
		body.append("</p>");
		return body.toString();
	}

	private String getRestoredAccountNotification(
			String userName,
			String accountno,
			String mdn,
			String esn,
			String restoreDate
			) {
		StringBuffer body = new StringBuffer();
		body.append("<div style=\"color:#306EFF; font-size:1.25em; background:#EAEAEA\"> \n");
		body.append("<b>Your TruConnect Service has been Restored</b> \n");
		body.append("</div> \n");
		body.append("<p> \n");
		body.append("	<b>Dear " + userName + ",</b> \n");
		body.append("</p> \n");
		body.append("<p>Services associated with your account " + accountno + " have been restored. Below you will find the device information regarding this restoral transaction.</p> \n");
		body.append(" \n");
		body.append("<p> \n");
		body.append("	<table border=\"1\"> \n");
		body.append("		<th>Account</th> \n");
		body.append("		<th>TN</th> \n");
		body.append("		<th>ESN</th> \n");
		body.append("		<th>Restore Date</th> \n");
		body.append("		<tr> \n");
		body.append("			<td>" + accountno + "</td> \n");
		body.append("			<td>" + mdn + "</td> \n");
		body.append("			<td>" + esn + "</td> \n");
		body.append("			<td>" + restoreDate + "</td> \n");
		body.append("		</tr> \n");
		body.append("	</table> \n");
		body.append("</p> \n");
		body.append(" \n");
		body.append("<p> \n");
		body.append("	<a href=\"https://manage.truconnect.com/truconnect/login\">Log in</a> and manage your billing and payment information \n");
		body.append("</p> \n");
		body.append("<p> \n");
		body.append("	Thank you for choosing TruConnect for your wireless and data needs. We value your business and look forward to serving you! \n");
		body.append("</p>");
		return body.toString();
	}
	
	private String getErrorBody( String userName, String accountno, String mdn, String esn, String action, String error) {
		StringBuffer body = new StringBuffer();
		body.append(" <div style=\"color:#306EFF; font-size:1.25em; background:#EAEAEA\"> "); 
		body.append(" <b>Your TruConnect Service has encountered an error</b> "); 
		body.append(" </div> "); 
		body.append(" <p> "); 
		body.append(" 	<b>Dear " + userName + ",</b> "); 
		body.append(" </p> "); 
		body.append(" <p>An error was encountered when processing your service.</p> "); 
		body.append(" <p>&nbsp;&nbsp;&nbsp;&nbsp;<i>" + error + "</i></p> "); 
		body.append(" <p>Below are the service details along with the action that was attempted against your account:</p> "); 
		body.append("  "); 
		body.append(" <p> "); 
		body.append(" 	<table border=\"1\"> "); 
		body.append(" 		<th>Account</th> "); 
		body.append(" 		<th>TN</th> "); 
		body.append(" 		<th>ESN</th> "); 
		body.append(" 		<th>Action</th> "); 
		body.append(" 		<tr> "); 
		body.append(" 			<td>" + accountno + "</td> "); 
		body.append(" 			<td>" + mdn + "</td> "); 
		body.append(" 			<td>" + esn + "</td> "); 
		body.append(" 			<td>" + action + "</td> "); 
		body.append(" 		</tr> "); 
		body.append(" 	</table> "); 
		body.append(" </p> "); 
		body.append("  "); 
		body.append(" <p> "); 
		body.append(" 	Please <a href=\"https://manage.truconnect.com/truconnect/login\">log in</a> and correct the issue at your earliest convenience or contact TruConnect customer service at 1-855-878-2666. "); 
		body.append(" </p> "); 
		body.append(" <p> "); 
		body.append(" 	Thank you for choosing TruConnect for your wireless and data needs. We value your business and look forward to serving you! "); 
		body.append(" </p> ");
		
		return body.toString();
	}
	
	private String getErrorBody( ProcessException process_ex ) {
		StringBuffer body = new StringBuffer();
		body.append(" <div style=\"color:#306EFF; font-size:1.25em; background:#EAEAEA\"> "); 
		body.append(" <b>Your TruConnect Service has encountered an error</b> "); 
		body.append(" </div> "); 
		body.append(" <p> "); 
		body.append(" 	<b>Dear " + process_ex.getAccount().getFirstname() + ",</b> "); 
		body.append(" </p> "); 
		body.append(" <p>An error was encountered when processing your service.</p> "); 
		body.append(" <p>&nbsp;&nbsp;&nbsp;&nbsp;<b><i>" + process_ex.getMessage() + "</i></b></p> "); 
		body.append(" <p>Below are the service details along with the action that was attempted against your account:</p> "); 
		body.append("  "); 
		body.append(" <p> "); 
		body.append(" 	<table border=\"1\"> "); 
		body.append(" 		<th>Account</th> "); 
		if( process_ex.getAccount().getFirstname() != null || process_ex.getAccount().getLastname() != null ) {
		body.append("       <th>Customer Name</th> ");
		}
		body.append(" 		<th>TN</th> "); 
		if( process_ex.getNetworkInfo() != null ) {
		body.append(" 		<th>DEVICE</th> "); 
		}
		if( process_ex.getAccount().getContactNumber() != null && process_ex.getAccount().getContactNumber().trim().length() > 0 ) {
		body.append(" 		<th>Contact Number</th> ");	
		}
		body.append(" 		<th>Action</th> "); 
		body.append(" 		<tr> "); 
		body.append(" 			<td>" + process_ex.getAccountNo() + "</td> ");
		if( process_ex.getAccount().getFirstname() != null || process_ex.getAccount().getLastname() != null ) {
		body.append("           <td>" + process_ex.getAccount().getFirstname()+" "+ process_ex.getAccount().getLastname() + "</td> ");
		}
		body.append(" 			<td>" + process_ex.getMdn() + "</td> "); 
		if( process_ex.getNetworkInfo() != null ) {
		body.append(" 			<td>" + process_ex.getNetworkInfo().getEsnmeiddec() + "</td> "); 
		} 
		if( process_ex.getAccount().getContactNumber() != null && process_ex.getAccount().getContactNumber().trim().length() > 0 ) {
		body.append(" 			<td>" + process_ex.getAccount().getContactNumber() + "</td> "); 
		}
		body.append(" 			<td>" + process_ex.getAction() + "</td> "); 
		body.append(" 		</tr> "); 
		body.append(" 	</table> "); 
		body.append(" </p> "); 
		body.append("  "); 
		body.append(" <p> "); 
		body.append(" 	Please <a href=\"https://manage.truconnect.com/truconnect/login\">log in</a> and correct the issue at your earliest convenience or contact TruConnect customer service at 1-855-878-2666. "); 
		body.append(" </p> "); 
		body.append(" <p> "); 
		body.append(" 	Thank you for choosing TruConnect for your wireless and data needs. We value your business and look forward to serving you! "); 
		body.append(" </p> ");
		
		return body.toString();
	}

	private CustBalance getCustBalance(int accountNo) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		
		CustBalance custBalance = null;
		
		Query q = session.getNamedQuery("get_cust_balance");
		q.setParameter("in_user_name", "tcbu");
		q.setParameter("in_account_no", accountNo);
		List<CustBalance> custBalanceList = q.list();
		if( custBalanceList != null && custBalanceList.size() > 0 ) {
			custBalance = custBalanceList.get(0);
		}
		
		session.getTransaction().commit();
		return custBalance;
	}
}
